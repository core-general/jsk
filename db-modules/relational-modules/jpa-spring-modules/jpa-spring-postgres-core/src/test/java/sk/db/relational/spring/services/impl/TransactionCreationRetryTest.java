package sk.db.relational.spring.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.testcontainers.postgresql.PostgreSQLContainer;
import sk.db.relational.spring.services.RdbTransactionWrapper;
import sk.services.async.ISleep;
import sk.services.retry.RepeatImpl;
import sk.utils.tuples.X;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionCreationRetryTest {
    static PostgreSQLContainer postgres;
    static LocalContainerEntityManagerFactoryBean factory;
    static EntityManager em;
    static RdbTransactionWrapper boundary;
    static RdbTransactionManagerImpl transactions;
    static JdbcTemplate jdbc;
    ISleep sleep;

    @BeforeAll static void start() {
        postgres = new PostgreSQLContainer("postgres:16-alpine");
        postgres.start();
        var source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(source);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setPackagesToScan(RetryRecord.class.getPackageName());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
        factory.afterPropertiesSet();
        em = SharedEntityManagerCreator.createSharedEntityManager(factory.getObject());
        var proxy = new ProxyFactory(new RdbTransactionWrapperImpl());
        proxy.addAdvice(new TransactionInterceptor(new JpaTransactionManager(factory.getObject()),
                new AnnotationTransactionAttributeSource()));
        boundary = (RdbTransactionWrapper) proxy.getProxy();
        transactions = new RdbTransactionManagerImpl(() -> em, boundary) {
            @Override protected void saveSingleItem(Object item) {
                var entityManager = em.getEntityManager();
                if (!entityManager.contains(item)) entityManager.persist(item);
            }
        };
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE deferred_record (id text PRIMARY KEY, natural_key text UNIQUE DEFERRABLE INITIALLY DEFERRED)");
        jdbc.execute("CREATE TABLE constrained_record (id text PRIMARY KEY, required text NOT NULL, positive int CHECK (positive > 0), parent text REFERENCES retry_record(id))");
    }

    @AfterAll static void stop() {
        if (factory != null) factory.destroy();
        if (postgres != null) postgres.stop();
    }

    @BeforeEach void reset() {
        jdbc.execute("TRUNCATE constrained_record, deferred_record, retry_record");
        sleep = mock(ISleep.class);
        transactions.retry = new RepeatImpl(sleep);
    }

    @Test void competingInsertAtFlushRollsBackAndReloads() throws Exception { raceCreation(true, false); }
    @Test void competingInsertAtCommitRollsBackAndReloads() throws Exception { raceCreation(false, false); }
    @Test void deferredUniqueConflictAtDatabaseCommitRollsBackAndReloads() throws Exception { raceCreation(false, true); }

    private void raceCreation(boolean flush, boolean deferred) throws Exception {
        var observedMissing = new CyclicBarrier(2);
        var attempts = new AtomicInteger();
        var sessions = ConcurrentHashMap.newKeySet();
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<String>> futures = new ArrayList<>();
            for (int worker = 0; worker < 2; worker++) {
                String owner = "worker-" + worker;
                var localAttempts = new AtomicInteger();
                futures.add(pool.submit(() -> transactions.transactionWithSaveX2(() -> {
                    attempts.incrementAndGet();
                    sessions.add(em.getDelegate());
                    var existing = deferred
                            ? jdbc.queryForList("SELECT id FROM deferred_record WHERE natural_key='shared'", String.class)
                            : em.createQuery("select r.id from RetryRecord r where r.key = :key", String.class)
                                    .setParameter("key", "shared").getResultList();
                    if (localAttempts.incrementAndGet() == 1) await(observedMissing);
                    if (!existing.isEmpty()) return X.x(List.<Object>of(), existing.getFirst());
                    em.persist(new RetryRecord("partial-" + owner, "partial-" + owner));
                    em.flush();
                    if (deferred) jdbc.update("INSERT INTO deferred_record VALUES (?, 'shared')", owner);
                    else em.persist(new RetryRecord(owner, "shared"));
                    if (flush) em.flush();
                    return X.x(List.<Object>of(), owner);
                }, result -> result.i1).i2));
            }
            String first = futures.getFirst().get(30, TimeUnit.SECONDS);
            assertEquals(first, futures.getLast().get(30, TimeUnit.SECONDS));
            assertEquals(3, attempts.get());
            assertEquals(attempts.get(), sessions.size());
            assertEquals(List.of("partial-" + first), jdbc.queryForList("SELECT id FROM retry_record WHERE natural_key LIKE 'partial-%'", String.class));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM " + (deferred ? "deferred_record" : "retry_record") + " WHERE natural_key='shared'", Integer.class));
        }
        verify(sleep).sleep(100);
    }

    @Test void optimisticUpdatesStillReloadBeforeRetry() throws Exception {
        transactions.transactionWithSaveX1(() -> X.x(new RetryRecord("row", "key")));
        var read = new CyclicBarrier(2);
        var calls = new AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                var local = new AtomicInteger();
                futures.add(pool.submit(() -> transactions.transactionWithSaveX1(() -> {
                    calls.incrementAndGet();
                    var row = em.find(RetryRecord.class, "row");
                    if (local.incrementAndGet() == 1) await(read);
                    row.value++;
                    return X.x(row);
                })));
            }
            for (var future : futures) future.get(30, TimeUnit.SECONDS);
        }
        assertEquals(3, calls.get());
        assertEquals(2, jdbc.queryForObject("SELECT value FROM retry_record WHERE id='row'", Integer.class));
    }

    @Test void joinedTransactionRunsOnceAndOwnerCanRetry() {
        var failure = new DuplicateKeyException("creation conflict");
        var innerCalls = new AtomicInteger();
        assertSame(failure, assertThrows(DuplicateKeyException.class, () -> boundary.transactional(() ->
                transactions.transactionWithSaveX1(() -> {
                    innerCalls.incrementAndGet(); throw failure;
                }))));
        assertEquals(1, innerCalls.get());
        verifyNoInteractions(sleep);
        var outerCalls = new AtomicInteger();
        transactions.transactionWithSaveX1(() -> {
            outerCalls.incrementAndGet();
            return transactions.transactionWithSaveX1(() -> {
                if (outerCalls.get() == 1) throw failure;
                return X.x(new RetryRecord("nested", "nested"));
            });
        });
        assertEquals(2, outerCalls.get());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM retry_record", Integer.class));
    }

    @Test void forceNewRetriesIndependentlyOfOuterRollback() {
        var calls = new AtomicInteger();
        var outerFailure = new IllegalArgumentException("outer business rule");
        assertSame(outerFailure, assertThrows(IllegalArgumentException.class, () -> boundary.transactional(() -> {
            em.persist(new RetryRecord("outer", "outer"));
            transactions.transactionWithSaveX1ForceNew(() -> {
                assertNull(em.find(RetryRecord.class, "outer"));
                if (calls.incrementAndGet() == 1) {
                    em.persist(new RetryRecord("partial", "partial"));
                    em.flush();
                    throw new DuplicateKeyException("race");
                }
                return X.x(new RetryRecord("inner", "inner"));
            });
            throw outerFailure;
        })));
        assertEquals(2, calls.get());
        assertEquals(List.of("inner"), jdbc.queryForList("SELECT id FROM retry_record", String.class));
    }

    @Test void classifiedAndWrappedConflictsRetryWithoutLeakingWrapper() {
        var batch = new BatchUpdateException();
        batch.setNextException(new SQLException("unique", "23505"));
        List<RuntimeException> failures = List.of(new DuplicateKeyException("unique"),
                new RuntimeException(new ConstraintViolationException("unique", new SQLException(),
                        ConstraintViolationException.ConstraintKind.UNIQUE, "constraint")),
                new RuntimeException(new SQLException("unique", "23505")),
                new RuntimeException(batch), new RuntimeException(new OptimisticLockException()));
        for (var failure : failures) {
            var calls = new AtomicInteger();
            transactions.transactionWithSaveX1(() -> {
                if (calls.incrementAndGet() == 1) throw failure;
                return X.x(List.of());
            });
            assertEquals(2, calls.get());
        }
    }

    @Test void exhaustedRetriesPreserveOriginalFailureAndLimit() {
        var failure = new RuntimeException(new SQLException("unique", "23505"));
        var calls = new AtomicInteger();
        assertSame(failure, assertThrows(RuntimeException.class, () -> transactions.transactionWithSaveX1(() -> {
            calls.incrementAndGet(); throw failure;
        })));
        assertEquals(50, calls.get());
        verify(sleep, atLeast(49)).sleep(100);
    }

    @Test void unrelatedDatabaseConstraintsFailOnceAndRollback() {
        for (String insert : List.of(
                "INSERT INTO constrained_record VALUES ('invalid', NULL, 1, NULL)",
                "INSERT INTO constrained_record VALUES ('invalid', 'value', 0, NULL)",
                "INSERT INTO constrained_record VALUES ('invalid', 'value', 1, 'absent')")) {
            var calls = new AtomicInteger();
            assertThrows(RuntimeException.class, () -> transactions.transactionWithSaveX1(() -> {
                calls.incrementAndGet();
                em.persist(new RetryRecord("partial", "partial")); em.flush();
                jdbc.update(insert);
                return X.x(List.of());
            }));
            assertEquals(1, calls.get());
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM retry_record", Integer.class));
        }
        verifyNoInteractions(sleep);
    }

    @Test void businessFailuresAndCyclicCauseChainsPropagateUnchanged() {
        var business = new IllegalArgumentException("invalid input");
        var first = new RuntimeException("cycle");
        var second = new SQLException("foreign key", "23503", first);
        first.initCause(second);
        var next = new SQLException("check", "23514");
        second.setNextException(next); next.setNextException(second);
        for (var failure : List.of(business, first, new ConstraintViolationException("unknown", new SQLException(), "constraint"))) {
            var calls = new AtomicInteger();
            assertSame(failure, assertThrows(RuntimeException.class, () -> transactions.transactionWithSaveX1(() -> {
                calls.incrementAndGet(); throw failure;
            })));
            assertEquals(1, calls.get());
        }
        verifyNoInteractions(sleep);
    }

    private static void await(CyclicBarrier barrier) {
        try { barrier.await(10, TimeUnit.SECONDS); }
        catch (Exception failure) { throw new IllegalStateException(failure); }
    }
}
