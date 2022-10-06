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
import sk.utils.async.IAsyncUtil;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class ScheduledExecutorServiceWrapperWithThrowable extends ScheduledExecutorServiceWrapper {
    public ScheduledExecutorServiceWrapperWithThrowable(ScheduledExecutorService executor) {
        super(executor);
    }

    @Override
    public @NotNull ScheduledFuture<?> schedule(@NotNull Runnable command, long delay, @NotNull TimeUnit unit) {
        return super.schedule(() -> IAsyncUtil.throwableProcessing(command, false), delay, unit);
    }

    @Override
    public @NotNull <V> ScheduledFuture<V> schedule(@NotNull Callable<V> callable, long delay, @NotNull TimeUnit unit) {
        return super.schedule(() -> IAsyncUtil.throwableProcessing(callable, false), delay, unit);
    }

    @Override
    public @NotNull ScheduledFuture<?> scheduleAtFixedRate(@NotNull Runnable command, long initialDelay, long period,
            @NotNull TimeUnit unit) {
        return super.scheduleAtFixedRate(() -> IAsyncUtil.throwableProcessing(command, false), initialDelay, period, unit);
    }

    @Override
    public @NotNull ScheduledFuture<?> scheduleWithFixedDelay(@NotNull Runnable command, long initialDelay, long delay,
            @NotNull TimeUnit unit) {
        return super.scheduleWithFixedDelay(() -> IAsyncUtil.throwableProcessing(command, false), initialDelay, delay, unit);
    }

    @Override
    public <T> Future<T> submit(@NotNull Callable<T> task) {
        return super.submit(() -> IAsyncUtil.throwableProcessing(task, false));
    }

    @Override
    public <T> Future<T> submit(@NotNull Runnable task, T result) {
        return super.submit(() -> IAsyncUtil.throwableProcessing(task, false), result);
    }

    @Override
    public Future<?> submit(@NotNull Runnable task) {
        return super.submit(() -> IAsyncUtil.throwableProcessing(task, false));
    }

    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return super.invokeAll(IAsyncUtil.toThrowableTasks(tasks, false));
    }

    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit)
            throws InterruptedException {
        return super.invokeAll(IAsyncUtil.toThrowableTasks(tasks, false), timeout, unit);
    }

    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return super.invokeAny(IAsyncUtil.toThrowableTasks(tasks, false));
    }

    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return super.invokeAny(IAsyncUtil.toThrowableTasks(tasks, false), timeout, unit);
    }

    @Override
    public void execute(@NotNull Runnable command) {
        super.execute(() -> IAsyncUtil.throwableProcessing(command, false));
    }
}
