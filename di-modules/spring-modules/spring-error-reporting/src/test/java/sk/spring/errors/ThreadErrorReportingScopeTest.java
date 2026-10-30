package sk.spring.errors;

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
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThreadErrorReportingScopeTest {
    @Test
    void nestedScopesRestoreTheirOwnLoggersAndNeverSuppressOtherThreads() throws Exception {
        var scopes = new ThreadErrorReportingScope();
        try (var threads = Executors.newSingleThreadExecutor(); var outer = scopes.suppressLoggers(Set.of("sql"))) {
            assertTrue(scopes.isSuppressed("sql"));
            try (var inner = scopes.suppressLoggers(Set.of("sql", "other"))) {
                assertTrue(scopes.isSuppressed("other"));
                assertFalse(threads.submit(() -> scopes.isSuppressed("sql")).get());
            }
            assertTrue(scopes.isSuppressed("sql"));
            assertFalse(scopes.isSuppressed("other"));
        }
        assertFalse(scopes.isSuppressed("sql"));
    }
}
