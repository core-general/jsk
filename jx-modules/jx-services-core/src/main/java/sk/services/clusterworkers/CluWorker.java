package sk.services.clusterworkers;

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

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import sk.services.async.IAsync;
import sk.services.clusterworkers.model.CluDelay;
import sk.services.clusterworkers.model.CluMessage;
import sk.services.clusterworkers.model.CluState;
import sk.services.time.ITime;
import sk.utils.async.ForeverThreadWithFinish;
import sk.utils.functional.*;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

@Log4j2
public abstract class CluWorker<S extends Enum<S> & CluState<S>, M extends CluMessage> {
    @Inject protected IAsync async;
    @Inject protected ITime times;
    protected final String name;

    private volatile S state;
    private final List<CluScheduler<S, ? extends M>> schedulers = Cc.l();
    private final BlockingQueue<M> messageQue = new LinkedBlockingQueue<>();
    private final ReentrantLock lock = new ReentrantLock(true);
    private ForeverThreadWithFinish processingThread;

    @SuppressWarnings("unused")
    CluWorker(String name, S initialState) {
        this.name = name;
        this.state = initialState;
    }

    CluWorker(String name, S initialState, IAsync async, ITime times) {
        this.name = name;
        this.state = initialState;
        this.async = async;
        this.times = times;
    }

    protected <MM extends M> CluScheduler<S, MM> addScheduler(String schedulerName,
            CluDelay delay, O<Set<S>> allowedStates, F0<O<MM>> processor, boolean dedicatedThread) {
        return inLock(() -> privateAddScheduler(schedulerName, allowedStates, processor, delay, dedicatedThread));
    }

    protected synchronized void start(C2<M, S> processor, C1<Throwable> errorConsumer) throws RuntimeException {
        inLock(() -> {
            if (processingThread != null && !processingThread.isFinished()) {
                stop(1000).thenAccept(w -> {
                    if (!w) { throw new RuntimeException("Can't start processingThread!"); }
                });
            }
            schedulers.forEach(CluScheduler::start);
            processingThread = new ForeverThreadWithFinish(() -> {
                try {
                    M msg = messageQue.poll(5, TimeUnit.MILLISECONDS);
                    if (msg != null) {
                        processor.accept(msg, getState());
                        log.trace(() -> name + " - processor for msg: " + msg + " finished");
                    }
                } catch (Exception e) {
                    try {
                        errorConsumer.accept(e);
                    } catch (Exception ex2) {
                        log.error("", ex2);
                    }
                    log.trace(() -> name + " - error:" + Ex.getInfo(e));
                }
            }, name + "_EventQueueProcessor", true);
            processingThread.start();
        });
    }

    @SuppressWarnings("WeakerAccess")
    public synchronized CompletableFuture<Boolean> stop(long msTimeout) {
        return inLock(() -> {
            schedulers.forEach(CluScheduler::stop);
            CompletableFuture<Boolean> complete = processingThread.finishThread(msTimeout);
            processingThread = null;
            return complete;
        });
    }

    protected void setState(S newState) {
        inLock(() -> {
            this.state = newState;
            log.debug(() -> name + " - State changed to:" + this.state);
        });
    }

    private S getState() {
        return state;
    }

    @SuppressWarnings("Convert2MethodRef")
    @NotNull
    private <MM extends M> CluScheduler<S, MM> privateAddScheduler(String schedulerName, O<Set<S>> allowedStates,
            F0<O<MM>> processor, CluDelay periodDelay, boolean dedicatedThread) {
        CluScheduler<S, MM> scheduler =
                new CluScheduler<>(name + "_" + schedulerName,
                        dedicatedThread
                                ? () -> async.newDedicatedScheduledExecutor(name + "_" + schedulerName)
                                : () -> new ScheduledExecutorServiceNoShutdownDecorator(async.scheduledExec()),
                        periodDelay, allowedStates,
                        () -> getState(), processor, messageQue::add);
        schedulers.add(scheduler);
        return scheduler;
    }

    private void inLock(R r) {
        try {
            lock.lock();
            r.run();
        } finally {
            lock.unlock();
        }
    }

    private <X> X inLock(Gett<X> r) {
        try {
            lock.lock();
            return r.get();
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("NullableProblems")
    private static class ScheduledExecutorServiceNoShutdownDecorator implements ScheduledExecutorService {
        private ScheduledExecutorService scheduledExec;

        public ScheduledExecutorServiceNoShutdownDecorator(ScheduledExecutorService scheduledExec) {
            this.scheduledExec = scheduledExec;
        }

        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return this.scheduledExec.schedule(command, delay, unit);
        }

        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return this.scheduledExec.schedule(callable, delay, unit);
        }

        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
                TimeUnit unit) {return this.scheduledExec.scheduleAtFixedRate(command, initialDelay, period, unit);}

        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
                TimeUnit unit) {return this.scheduledExec.scheduleWithFixedDelay(command, initialDelay, delay, unit);}

        public void shutdown() { }

        public List<Runnable> shutdownNow() {return Cc.lEmpty();}

        public boolean isShutdown() {return this.scheduledExec.isShutdown();}

        public boolean isTerminated() {return this.scheduledExec.isTerminated();}

        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return this.scheduledExec.awaitTermination(timeout, unit);
        }

        public <T> Future<T> submit(Callable<T> task) {return this.scheduledExec.submit(task);}

        public <T> Future<T> submit(Runnable task, T result) {return this.scheduledExec.submit(task, result);}

        public Future<?> submit(Runnable task) {return this.scheduledExec.submit(task);}

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
                throws InterruptedException {return this.scheduledExec.invokeAll(tasks);}

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {return this.scheduledExec.invokeAll(tasks, timeout, unit);}

        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {return this.scheduledExec.invokeAny(tasks);}

        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException,
                TimeoutException {return this.scheduledExec.invokeAny(tasks, timeout, unit);}

        public void execute(Runnable command) {this.scheduledExec.execute(command);}
    }
}
