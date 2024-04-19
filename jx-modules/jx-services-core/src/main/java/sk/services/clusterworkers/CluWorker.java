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

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import sk.services.async.IAsync;
import sk.services.async.ScheduledExecutorServiceWrapperWithThrowable;
import sk.services.clusterworkers.model.CluDelay;
import sk.services.clusterworkers.model.CluMessage;
import sk.services.clusterworkers.model.CluState;
import sk.services.time.ITime;
import sk.utils.async.ForeverThreadWithFinish;
import sk.utils.functional.*;
import sk.utils.statics.Cc;

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class CluWorker<STATE extends Enum<STATE> & CluState<STATE>, MSG extends CluMessage> {
    @Inject protected IAsync async;
    @Inject protected ITime times;
    protected final String name;

    private volatile STATE state;
    private final List<CluScheduler<STATE, ? extends MSG>> schedulers = Cc.l();
    private final BlockingQueue<MSG> messageQue = new LinkedBlockingQueue<>();
    private final ReentrantLock lock = new ReentrantLock(true);
    private ForeverThreadWithFinish processingThread;

    @SuppressWarnings("unused")
    CluWorker(String name, STATE initialState) {
        this.name = name;
        this.state = initialState;
    }

    CluWorker(String name, STATE initialState, IAsync async, ITime times) {
        this.name = name;
        this.state = initialState;
        this.async = async;
        this.times = times;
    }

    protected <MM extends MSG> CluScheduler<STATE, MM> addScheduler(String schedulerName,
            CluDelay delay, O<Set<STATE>> allowedStates, F0<O<MM>> processor, boolean dedicatedThread) {
        return inLock(() -> privateAddScheduler(schedulerName, allowedStates, processor, delay, dedicatedThread));
    }

    protected synchronized void start(C2<MSG, STATE> processor, C1<Throwable> errorConsumer) throws RuntimeException {
        inLock(() -> {
            if (processingThread != null && !processingThread.isFinished()) {
                stop(1000).thenAccept(w -> {
                    if (!w) {throw new RuntimeException("Can't start processingThread!");}
                });
            }
            schedulers.forEach(CluScheduler::start);
            processingThread = new ForeverThreadWithFinish(() -> {
                try {
                    MSG msg = messageQue.poll(3, TimeUnit.SECONDS);
                    if (msg != null) {
                        processor.accept(msg, getState());
                        //log.trace(() -> name + " - processor for msg: " + msg + " finished");
                    }
                } catch (Exception e) {
                    try {
                        errorConsumer.accept(e);
                    } catch (Exception ex2) {
                        log.error("", e);
                        log.error("", ex2);
                    }
                    //log.trace(() -> name + " - error:" + Ex.getInfo(e));
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

    protected void setState(STATE newState) {
        inLock(() -> {
            this.state = newState;
            if (log.isDebugEnabled()) {
                log.debug(name + " - State changed to:" + this.state);
            }
        });
    }

    private STATE getState() {
        return state;
    }

    @SuppressWarnings("Convert2MethodRef")

    private <MM extends MSG> CluScheduler<STATE, MM> privateAddScheduler(String schedulerName, O<Set<STATE>> allowedStates,
            F0<O<MM>> processor, CluDelay periodDelay, boolean dedicatedThread) {
        CluScheduler<STATE, MM> scheduler =
                new CluScheduler<>(name + "_" + schedulerName,
                        dedicatedThread
                        ? () -> async.newDedicatedScheduledExecutor(name + "_" + schedulerName).getUnderlying()
                        : () -> new ScheduledExecutorServiceNoShutdownDecorator(async.scheduledExec().getUnderlying()),
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
    private static class ScheduledExecutorServiceNoShutdownDecorator extends ScheduledExecutorServiceWrapperWithThrowable {
        public ScheduledExecutorServiceNoShutdownDecorator(ScheduledExecutorService scheduledExec) {
            super(scheduledExec);
        }

        public void shutdown() {}

        public List<Runnable> shutdownNow() {return Cc.lEmpty();}
    }
}
