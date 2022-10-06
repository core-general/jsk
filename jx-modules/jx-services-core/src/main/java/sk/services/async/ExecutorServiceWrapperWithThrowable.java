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

public class ExecutorServiceWrapperWithThrowable extends ExecutorServiceWrapper {
    public ExecutorServiceWrapperWithThrowable(ExecutorService executor) {
        super(executor);
    }

    @Override
    public <T> Future<T> submit(@NotNull Callable<T> task) {
        return super.submit(() -> IAsyncUtil.throwableProcessing(task, true));
    }

    @Override
    public <T> Future<T> submit(@NotNull Runnable task, T result) {
        return super.submit(() -> IAsyncUtil.throwableProcessing(task, true), result);
    }

    @Override
    public Future<?> submit(@NotNull Runnable task) {
        return super.submit(() -> IAsyncUtil.throwableProcessing(task, true));
    }

    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return super.invokeAll(IAsyncUtil.toThrowableTasks(tasks, true));
    }

    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit)
            throws InterruptedException {
        return super.invokeAll(IAsyncUtil.toThrowableTasks(tasks, true), timeout, unit);
    }

    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return super.invokeAny(IAsyncUtil.toThrowableTasks(tasks, true));
    }

    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return super.invokeAny(IAsyncUtil.toThrowableTasks(tasks, true), timeout, unit);
    }

    @Override
    public void execute(@NotNull Runnable command) {
        super.execute(() -> IAsyncUtil.throwableProcessing(command, true));
    }
}
