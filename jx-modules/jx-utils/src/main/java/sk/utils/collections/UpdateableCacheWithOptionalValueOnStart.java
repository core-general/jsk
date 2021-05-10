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
import sk.utils.functional.O;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The goal of this cache is the same
 * The cache stores values forever and is not bounded in any way.
 */
public class UpdateableCacheWithOptionalValueOnStart<V> {
    private volatile O<V> value;
    private final ScheduledFuture<?> future;

    public UpdateableCacheWithOptionalValueOnStart(F0<V> cacheUpdater, C1<Exception> onError, ScheduledExecutorService executor,
            Duration repeatDescription) {
        value = O.empty();

        try {
            value = O.ofNull(cacheUpdater.apply());
        } catch (Exception e) {
            onError.accept(e);
        }

        future = executor.scheduleWithFixedDelay(() -> {
            try {
                value = O.ofNull(cacheUpdater.apply());
            } catch (Exception e) {
                onError.accept(e);
            }
        }, repeatDescription.toMillis(), repeatDescription.toMillis(), TimeUnit.MILLISECONDS);
    }

    public O<V> get() {
        return value;
    }

    public void stop() {
        future.cancel(true);
    }
}
