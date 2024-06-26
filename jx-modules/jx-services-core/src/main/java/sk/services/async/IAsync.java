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
import sk.utils.functional.F1;
import sk.utils.functional.R;
import sk.utils.statics.Ex;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.allOf;

@SuppressWarnings("unused")
public interface IAsync extends ISleep {

    IExecutorService fixedExec();

    IExecutorService fixedExec(int threads);

    IExecutorService bufExec();

    IExecutorService singleExec();

    IScheduledExecutorService scheduledExec();

    IExecutorService coldTaskFJP();

    IScheduledExecutorService newDedicatedScheduledExecutor(String name);

    default Thread runNamedThread(String name, R task, boolean daemon) {
        Thread thread = new Thread(task, "JfgElnAntibodiesProviderImpl");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    default void coldTaskFJPRun(int threads, R toRun) {
        coldTaskFJPGet(threads, () -> {
            toRun.run();
            return null;
        });
    }

    default void coldTaskFJPRun(R toRun) {
        coldTaskFJPGet(() -> {
            toRun.run();
            return null;
        });
    }

    <T> T coldTaskFJPGet(int threads, F0<T> toRun);

    default <T> T coldTaskFJPGet(F0<T> toRun) {
        return Ex.toRuntime(() -> coldTaskFJP().call(toRun::apply).get());
    }

    default CompletableFuture<Void> runAsyncDontWait(List<R> toRun) {
        return runAsyncDontWait(toRun, this::runBuf);
    }

    default <U> CompletableFuture<List<U>> supplyAsyncDontWait(List<F0<U>> toRun) {
        return supplyAsyncDontWait(toRun, this::supplyBuf);
    }

    default <U> List<CompletableFuture<U>> supplyToList(List<F0<U>> toRun) {
        return toRun.stream()
                .map(this::supplyBuf)
                .toList();
    }

    default CompletableFuture<Void> runAsyncDontWait(List<R> toRun, int threadCount) {
        return runAsyncDontWait(toRun, run -> runFix(run, threadCount));
    }

    default <U> CompletableFuture<List<U>> supplyAsyncDontWait(List<F0<U>> toRun, int threadCount) {
        return supplyAsyncDontWait(toRun, run -> supplyFix(run, threadCount));
    }

    default <U> List<CompletableFuture<U>> supplyToList(List<F0<U>> toRun, int threadCount) {
        return toRun.stream()
                .map(run -> supplyFix(run, threadCount))
                .toList();
    }

    default CompletableFuture<Void> runAsyncDontWait(List<R> toRun, F1<R, CompletableFuture<Void>> runner) {
        return allOf(toRun.stream()
                .map(runner)
                .toArray(CompletableFuture[]::new));
    }

    default <U> CompletableFuture<List<U>> supplyAsyncDontWait(List<F0<U>> toRun, F1<F0<U>, CompletableFuture<U>> runner) {
        List<CompletableFuture<U>> completableFutures = supplyToList(toRun, runner);

        return allOf(completableFutures.toArray(CompletableFuture[]::new))
                .thenApply(unused -> completableFutures.stream().map($ -> $.join()).toList());
    }

    default <U> List<CompletableFuture<U>> supplyToList(List<F0<U>> toRun, F1<F0<U>, CompletableFuture<U>> runner) {
        return toRun.stream()
                .map(runner)
                .toList();
    }

    default CompletableFuture<Void> runBuf(R run) {
        return CompletableFuture.runAsync(run, bufExec().getUnderlying());
    }

    default <U> CompletableFuture<U> supplyBuf(F0<U> run) {
        return CompletableFuture.supplyAsync(run, bufExec().getUnderlying());
    }

    default CompletableFuture<Void> runFix(R run, int threads) {
        return CompletableFuture.runAsync(run, fixedExec(threads).getUnderlying());
    }

    default <U> CompletableFuture<U> supplyFix(F0<U> run, int threads) {
        return CompletableFuture.supplyAsync(run, fixedExec(threads).getUnderlying());
    }

    /** Will return first successful or fail if all failed */
    public default <T> CompletableFuture<T> anyNonFailed(List<CompletableFuture<T>> futures) {
        if (futures.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        // Create a new CompletableFuture that will be manually completed
        CompletableFuture<T> firstSuccessful = new CompletableFuture<>();

        AtomicInteger ai = new AtomicInteger(futures.size());
        futures.forEach(future -> future.whenComplete((result, ex) -> {
            if (result != null) {
                firstSuccessful.complete(result);
            }
            // if all failed, then we need to complete exceptionally on the returned future
            if (ai.decrementAndGet() == 0 && !firstSuccessful.isDone()) {
                firstSuccessful.completeExceptionally(ex);
            }
        }));

        return firstSuccessful;
    }

    default <T> List<T> supplyParallel(List<F0<T>> suppliers) {
        val job = suppliers.stream().map(this::supplyBuf).collect(Collectors.toList());

        allOf(job.toArray(new CompletableFuture[0])).join();

        return job.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    default void runParallel(List<R> toRun) {
        val cb = new CyclicBarrier(toRun.size() + 1);
        AtomicReference<RuntimeException> exception = new AtomicReference<>();
        toRun.forEach(R -> bufExec().submit(() -> {
            try {
                R.run();
            } catch (RuntimeException e) {
                exception.compareAndSet(null, e);
            } finally {
                try {
                    cb.await();
                } catch (Exception ignored) {
                }
            }
        }));
        Ex.toRuntime(() -> cb.await());
        if (exception.get() != null) {
            throw exception.get();
        }
    }

    default JskBarrier barrier(int partiesNumber) {
        return new JskBarrierWrapper(partiesNumber);
    }

    default JskSemaphore semaphore(int partiesNumber) {
        return new JskSemaphoreWrapper(partiesNumber);
    }

    void stop();

    public static interface JskBarrier {
        public void reset();

        public int await();
    }

    public static interface JskSemaphore {
        public boolean tryAcquire();

        public void release();

        boolean tryAcquire(long timeout, TimeUnit unit);

        void acquire(int permits);

        boolean tryAcquire(int permits);

        boolean tryAcquire(int permits, long timeout, TimeUnit unit);

        void release(int permits);

        int availablePermits();
    }

    class JskBarrierWrapper implements JskBarrier {
        private final CyclicBarrier cyclicBarrier;

        public JskBarrierWrapper(int partiesNumber) {
            cyclicBarrier = new CyclicBarrier(partiesNumber);
        }

        public int getParties() {return this.cyclicBarrier.getParties();}

        @SneakyThrows
        public int await() {
            return this.cyclicBarrier.await();
        }

        @SneakyThrows
        public int await(long timeout, TimeUnit unit) {return this.cyclicBarrier.await(timeout, unit);}

        public boolean isBroken() {return this.cyclicBarrier.isBroken();}

        public void reset() {this.cyclicBarrier.reset();}

        public int getNumberWaiting() {return this.cyclicBarrier.getNumberWaiting();}
    }

    class JskSemaphoreWrapper implements JskSemaphore {
        private final Semaphore sema;

        public JskSemaphoreWrapper(int partiesNumber) {
            sema = new Semaphore(partiesNumber, true);
        }

        public void acquire() throws InterruptedException {this.sema.acquire();}

        public void acquireUninterruptibly() {this.sema.acquireUninterruptibly();}

        public boolean tryAcquire() {return this.sema.tryAcquire();}

        @SneakyThrows
        public boolean tryAcquire(long timeout, TimeUnit unit) {
            return this.sema.tryAcquire(timeout, unit);
        }

        public void release() {this.sema.release();}

        @SneakyThrows
        public void acquire(int permits) {this.sema.acquire(permits);}

        public void acquireUninterruptibly(int permits) {this.sema.acquireUninterruptibly(permits);}

        public boolean tryAcquire(int permits) {return this.sema.tryAcquire(permits);}

        @SneakyThrows
        public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
            return this.sema.tryAcquire(permits, timeout, unit);
        }

        public void release(int permits) {this.sema.release(permits);}

        public int availablePermits() {return this.sema.availablePermits();}

        public int drainPermits() {return this.sema.drainPermits();}

        public boolean isFair() {return this.sema.isFair();}

        public boolean hasQueuedThreads() {return this.sema.hasQueuedThreads();}

        public int getQueueLength() {return this.sema.getQueueLength();}
    }
}
