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

import java.util.Set;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import sk.services.errors.IErrorReportingScope;

public class TransactionSqlReportingScope implements AutoCloseable, TransactionSynchronization {
    public static final String SQL_EXCEPTION_LOGGER = "org.hibernate.engine.jdbc.spi.SqlExceptionHelper";
    private static final Set<String> LOGGERS = Set.of(SQL_EXCEPTION_LOGGER);
    private final IErrorReportingScope scopes;
    private final boolean joined;
    private IErrorReportingScope.Scope suppression;

    public TransactionSqlReportingScope(IErrorReportingScope scopes, boolean joining) {
        this.scopes = scopes;
        joined = joining && TransactionSynchronizationManager.isSynchronizationActive();
        suppression = scopes.suppressLoggers(LOGGERS);
        if (joined) TransactionSynchronizationManager.registerSynchronization(this);
    }

    @Override
    public void close() {
        if (!joined) release();
    }

    @Override
    public void suspend() {
        release();
    }

    @Override
    public void resume() {
        suppression = scopes.suppressLoggers(LOGGERS);
    }

    @Override
    public void afterCompletion(int status) {
        release();
    }

    private void release() {
        if (suppression != null) {
            suppression.close();
            suppression = null;
        }
    }
}
