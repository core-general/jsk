package sk.services.async;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

abstract class ScheduledExecutorServiceWrapper extends ExecutorServiceWrapper implements ScheduledExecutorService {
    public ScheduledExecutorServiceWrapper(ScheduledExecutorService executor) {
        super(executor);
    }

    @NotNull
    @Override
    public ScheduledFuture<?> schedule(@NotNull Runnable command, long delay, @NotNull TimeUnit unit) {
        return ((ScheduledExecutorService) executor).schedule(command, delay, unit);
    }

    @NotNull
    @Override
    public <V> ScheduledFuture<V> schedule(@NotNull Callable<V> callable, long delay, @NotNull TimeUnit unit) {
        return ((ScheduledExecutorService) executor).schedule(callable, delay, unit);
    }

    @NotNull
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@NotNull Runnable command, long initialDelay, long period,
            @NotNull TimeUnit unit) {
        return ((ScheduledExecutorService) executor).scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @NotNull
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull Runnable command, long initialDelay, long delay,
            @NotNull TimeUnit unit) {
        return ((ScheduledExecutorService) executor).scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }
}
