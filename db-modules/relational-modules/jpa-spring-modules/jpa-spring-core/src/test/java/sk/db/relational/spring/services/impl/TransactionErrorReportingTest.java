package sk.db.relational.spring.services.impl;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2026 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import ch.qos.logback.classic.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionTemplate;
import sk.db.relational.spring.services.RdbTransactionWrapper;
import sk.services.async.ISleep;
import sk.services.retry.RepeatImpl;
import sk.spring.errors.ErrorReportingLogbackAppender;
import sk.spring.errors.ILoggedErrorReporter;
import sk.spring.errors.ThreadErrorReportingScope;
import sk.test.JskMockitoTest;
import sk.utils.tuples.X;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static sk.db.relational.spring.services.impl.TransactionSqlReportingScope.SQL_EXCEPTION_LOGGER;

class TransactionErrorReportingTest extends JskMockitoTest {
    @Mock ILoggedErrorReporter reporter;
    @Mock ISleep sleep;
    private final ThreadErrorReportingScope scopes = new ThreadErrorReportingScope();
    private final ProbeTransactions transactions = new ProbeTransactions();
    private final TransactionTemplate outer = new TransactionTemplate(transactions);
    private final TransactionTemplate independent = new TransactionTemplate(transactions);
    private RdbTransactionManagerGenericImpl manager;
    private ErrorReportingLogbackAppender appender;
    private Logger root;

    @BeforeEach
    void setup() {
        independent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        manager = new RdbTransactionManagerGenericImpl(null, new RdbTransactionWrapper() {
            @Override public <T> T transactional(Supplier<T> work) { return outer.execute(status -> work.get()); }
            @Override public void transactionalRun(Runnable work) { outer.executeWithoutResult(status -> work.run()); }
            @Override public <T> T transactionalForceNew(Supplier<T> work) { return independent.execute(status -> work.get()); }
            @Override public void transactionalRunForceNew(Runnable work) { independent.executeWithoutResult(status -> work.run()); }
        });
        manager.retry = new RepeatImpl(sleep);
        manager.errorReportingScopes = Optional.of(scopes);
        root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        appender = new ErrorReportingLogbackAppender(reporter, scopes);
        appender.setContext(root.getLoggerContext());
        appender.start();
        root.addAppender(appender);
    }

    @AfterEach
    void teardown() {
        root.detachAppender(appender);
        appender.stop();
        assertFalse(scopes.isSuppressed(SQL_EXCEPTION_LOGGER));
    }

    @Test
    void successfulRetrySuppressesAttemptDiagnosticsThroughCommitAndRollback() {
        transactions.failuresRemaining = 1;
        var attempts = new AtomicInteger();
        var result = manager.transactionWithSaveX1(() -> {
            attempts.incrementAndGet();
            assertTrue(scopes.isSuppressed(SQL_EXCEPTION_LOGGER));
            return X.x(List.of());
        });
        assertNotNull(result);
        assertEquals(2, attempts.get());
        assertTrue(transactions.commitScopes.stream().allMatch(Boolean::booleanValue));
        assertEquals(List.of(true), transactions.rollbackScopes);
        verifyNoInteractions(reporter);
    }

    @Test
    void exhaustedAttemptDiagnosticsLeaveReportingToTheEscapingFailureBoundary() {
        transactions.failuresRemaining = Integer.MAX_VALUE;
        DuplicateKeyException failure = assertThrows(DuplicateKeyException.class,
                () -> manager.transactionWithSaveX1(() -> X.x(List.of())));
        assertSame(transactions.failure, failure);
        assertTrue(transactions.commitScopes.size() > 1);
        assertTrue(transactions.commitScopes.stream().allMatch(Boolean::booleanValue));
        assertEquals(transactions.commitScopes.size(), transactions.rollbackScopes.size());
        verifyNoInteractions(reporter);
        LoggerFactory.getLogger("test.request.boundary").error("request failed", failure);
        verify(reporter).report(null, failure);
        verifyNoMoreInteractions(reporter);
    }

    @Test
    void joinedScopesFollowOuterCompletionAndAreSuspendedForIndependentTransactions() {
        outer.executeWithoutResult(status -> {
            manager.transactionWithSaveX1(() -> X.x(List.of()));
            assertTrue(scopes.isSuppressed(SQL_EXCEPTION_LOGGER));
            independent.executeWithoutResult(inner -> assertFalse(scopes.isSuppressed(SQL_EXCEPTION_LOGGER)));
            assertTrue(scopes.isSuppressed(SQL_EXCEPTION_LOGGER));
            manager.transactionWithSaveX1ForceNew(() -> {
                assertTrue(scopes.isSuppressed(SQL_EXCEPTION_LOGGER));
                return X.x(List.of());
            });
            assertTrue(scopes.isSuppressed(SQL_EXCEPTION_LOGGER));
            manager.transactionWithSaveX1(() -> X.x(List.of()));
        });
        assertFalse(scopes.isSuppressed(SQL_EXCEPTION_LOGGER));
        assertEquals(List.of(false, true, true), transactions.commitScopes);
    }

    @Test
    void joinedDeferredCommitFailuresAreSuppressedAndNeverRetriedByTheInnerCall() {
        transactions.failuresRemaining = 1;
        var attempts = new AtomicInteger();
        DuplicateKeyException failure = assertThrows(DuplicateKeyException.class, () -> outer.executeWithoutResult(status ->
                manager.transactionWithSaveX1(() -> {
                    attempts.incrementAndGet();
                    return X.x(List.of());
                })));
        assertSame(transactions.failure, failure);
        assertEquals(1, attempts.get());
        assertEquals(List.of(true), transactions.commitScopes);
        assertEquals(List.of(true), transactions.rollbackScopes);
        verifyNoInteractions(reporter);
    }

    @Test
    void reportingIsOptionalAndDoesNotChangeRetryDecisions() {
        manager.errorReportingScopes = Optional.empty();
        transactions.failuresRemaining = 1;
        manager.transactionWithSaveX1(() -> X.x(List.of()));
        assertEquals(List.of(false, false), transactions.commitScopes);
        verify(reporter).reportLogged(eq(SQL_EXCEPTION_LOGGER), any(), eq("duplicate key"), eq("duplicate key"));
    }

    private class ProbeTransactions extends AbstractPlatformTransactionManager {
        private final ThreadLocal<Tx> current = new ThreadLocal<>();
        private final List<Boolean> commitScopes = new ArrayList<>();
        private final List<Boolean> rollbackScopes = new ArrayList<>();
        private final DuplicateKeyException failure = new DuplicateKeyException("conflict");
        private int failuresRemaining;

        private ProbeTransactions() { setRollbackOnCommitFailure(true); }
        @Override protected Object doGetTransaction() { return new Holder(current.get()); }
        @Override protected boolean isExistingTransaction(Object transaction) { return ((Holder) transaction).tx != null; }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
            Holder holder = (Holder) transaction;
            holder.tx = new Tx();
            current.set(holder.tx);
        }
        @Override protected Object doSuspend(Object transaction) {
            Holder holder = (Holder) transaction;
            Tx previous = holder.tx;
            holder.tx = null;
            current.remove();
            return previous;
        }
        @Override protected void doResume(Object transaction, Object suspended) {
            current.set((Tx) suspended);
            if (transaction != null) ((Holder) transaction).tx = (Tx) suspended;
        }
        @Override protected void doCommit(DefaultTransactionStatus status) {
            commitScopes.add(scopes.isSuppressed(SQL_EXCEPTION_LOGGER));
            if (failuresRemaining-- > 0) {
                LoggerFactory.getLogger(SQL_EXCEPTION_LOGGER).error("duplicate key");
                throw failure;
            }
        }
        @Override protected void doRollback(DefaultTransactionStatus status) {
            rollbackScopes.add(scopes.isSuppressed(SQL_EXCEPTION_LOGGER));
        }
        @Override protected void doSetRollbackOnly(DefaultTransactionStatus status) { ((Holder) status.getTransaction()).tx.rollbackOnly = true; }
        @Override protected void doCleanupAfterCompletion(Object transaction) { current.remove(); }
    }

    private static class Tx { boolean rollbackOnly; }
    private static class Holder implements SmartTransactionObject {
        private Tx tx;
        private Holder(Tx tx) { this.tx = tx; }
        @Override public boolean isRollbackOnly() { return tx != null && tx.rollbackOnly; }
        @Override public void flush() {}
    }
}
