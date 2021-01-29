package sk.services.async;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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

import lombok.SneakyThrows;
import lombok.val;
import sk.utils.functional.F0;
import sk.utils.functional.R;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.allOf;

@SuppressWarnings("unused")
public interface IAsync extends ISleep {

    IExecutorService fixedExec();

    IExecutorService fixedExec(int threads);

    IExecutorService bufExec();

    IExecutorService singleExec();

    ScheduledExecutorService scheduledExec();

    IExecutorService coldTaskFJP();

    ScheduledExecutorService newDedicatedScheduledExecutor(String name);

    default void coldTaskFJPRun(int threads, R toRun) {
        final ForkJoinPool forkJoinPool = new ForkJoinPool(threads);
        try {
            forkJoinPool.submit(toRun).join();
        } finally {
            forkJoinPool.shutdown();
        }
    }

    @SneakyThrows
    default void coldTaskFJPRun(R toRun) {
        coldTaskFJP().submit(toRun).get();
    }

    default CompletableFuture<Void> runBuf(R run) {
        return CompletableFuture.runAsync(run, bufExec().getUnderlying());
    }

    default <U> CompletableFuture<U> supplyBuf(F0<U> run) {
        return CompletableFuture.supplyAsync(run, bufExec().getUnderlying());
    }

    void stop();

    @SneakyThrows
    default <T> List<T> supplyParallel(List<F0<T>> suppliers) {
        val job = suppliers.stream().map(this::supplyBuf).collect(Collectors.toList());

        allOf(job.toArray(new CompletableFuture[0])).join();

        return job.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    @SneakyThrows
    default void runParallel(List<R> toRun) {
        val cb = new CyclicBarrier(toRun.size() + 1);
        AtomicReference<Exception> exception = new AtomicReference<>();
        toRun.forEach(R -> bufExec().submit(() -> {
            try {
                R.run();
            } catch (Exception e) {
                exception.compareAndSet(null, e);
            } finally {
                try {
                    cb.await();
                } catch (Exception ignored) {
                }
            }
        }));
        cb.await();
        if (exception.get() != null) {
            throw exception.get();
        }
    }

    @SneakyThrows
    default CompletableFuture<Void> runAsyncDontWait(List<R> toRun) {
        return allOf(toRun.stream()
                .map(this::runBuf)
                .toArray(CompletableFuture[]::new));
    }
}
