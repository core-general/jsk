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

import lombok.AllArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

@AllArgsConstructor
abstract class ExecutorServiceWrapper implements ExecutorService {
    protected ExecutorService executor;

    public void shutdown() {executor.shutdown();}

    public List<Runnable> shutdownNow() {return executor.shutdownNow();}

    public boolean isShutdown() {return executor.isShutdown();}

    public boolean isTerminated() {return executor.isTerminated();}

    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {return executor.awaitTermination(timeout, unit);}

    public <T> Future<T> submit(Callable<T> task) {return executor.submit(task);}

    public <T> Future<T> submit(Runnable task, T result) {return executor.submit(task, result);}

    public Future<?> submit(Runnable task) {return executor.submit(task);}

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {return executor.invokeAll(tasks);}

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {return executor.invokeAll(tasks, timeout, unit);}

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {return executor.invokeAny(tasks);}

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {return executor.invokeAny(tasks, timeout, unit);}

    public void execute(Runnable command) {executor.execute(command);}
}
