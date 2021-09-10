package sk.utils.collections;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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

import sk.utils.functional.C1;
import sk.utils.functional.F0;
import sk.utils.functional.Gett;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The goal of this cache is:
 * 1. Populate cache with initial values, if it fails initial population - throw and exception.
 * 2. In normal scenario we provide the last successfully obtained value and schedule a task to repopulate cache.
 * 3. If repopulation fails we use fail hook to inform about it, but do not throw an exception.
 *
 * The cache stores values forever and is not bounded in any way.
 */
public class UpdateableCacheWithMustValueOnStart<V> implements Gett<V> {
    private volatile V value;
    private final ScheduledFuture<?> future;

    public UpdateableCacheWithMustValueOnStart(F0<V> cacheUpdater, C1<Exception> onError, ScheduledExecutorService executor,
            Duration repeatDescription) {
        value = cacheUpdater.apply();
        future = executor.scheduleWithFixedDelay(() -> {
            try {
                value = cacheUpdater.apply();
            } catch (Exception e) {
                onError.accept(e);
            }
        }, repeatDescription.toMillis(), repeatDescription.toMillis(), TimeUnit.MILLISECONDS);
    }

    public V get() {
        return value;
    }

    public void stop() {
        future.cancel(true);
    }
}
