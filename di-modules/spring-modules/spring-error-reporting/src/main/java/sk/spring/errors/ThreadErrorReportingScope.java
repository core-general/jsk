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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import sk.services.errors.IErrorReportingScope;

public class ThreadErrorReportingScope implements IErrorReportingScope {
    private final ThreadLocal<Map<String, Integer>> suppressed = new ThreadLocal<>();

    @Override
    public Scope suppressLoggers(Set<String> loggers) {
        Set<String> names = Set.copyOf(loggers);
        Map<String, Integer> counts = suppressed.get();
        if (counts == null) {
            counts = new HashMap<>();
            suppressed.set(counts);
        }
        for (String logger : names) counts.merge(logger, 1, Integer::sum);
        return new Suppression(names);
    }

    @Override
    public boolean isSuppressed(String logger) {
        Map<String, Integer> counts = suppressed.get();
        return counts != null && counts.containsKey(logger);
    }

    private class Suppression implements Scope {
        private final Set<String> names;
        private boolean closed;

        private Suppression(Set<String> names) {
            this.names = names;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            Map<String, Integer> counts = suppressed.get();
            if (counts == null) return;
            for (String logger : names) counts.computeIfPresent(logger, (key, count) -> count == 1 ? null : count - 1);
            if (counts.isEmpty()) suppressed.remove();
        }
    }
}
