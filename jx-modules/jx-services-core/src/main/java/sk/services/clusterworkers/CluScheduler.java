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
import sk.services.clusterworkers.model.CluDelay;
import sk.services.clusterworkers.model.CluMessage;
import sk.services.clusterworkers.model.CluState;
import sk.utils.functional.F0;
import sk.utils.functional.O;
import sk.utils.functional.R;
import sk.utils.functional.Sett;
import sk.utils.tuples.X;
import sk.utils.tuples.X1;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("WeakerAccess")
@Log4j2
public class CluScheduler<S extends Enum<S> & CluState<S>, M extends CluMessage> {
    private final R taskRestarter;
    private ScheduledFuture<?> currentTask;
    private ScheduledExecutorService currentExecutor;
    private AtomicLong expectedTaskId = new AtomicLong(0);

    private final Object mainTaskLock = new Object();
    private final Object auxiliaryLock = new Object();
    private final String schedulerName;
    private boolean started = false;

    CluScheduler(String schedulerName, F0<ScheduledExecutorService> executor,
            CluDelay delayProvider, O<Set<S>> allowedStates, F0<S> stateGetter,
            F0<O<M>> processor, Sett<M> resultPublisher) {
        this.schedulerName = schedulerName;

        recreateExecutor(executor);

        taskRestarter = () -> {
            synchronized (auxiliaryLock) {
                //log.debug(() -> schedulerName + " - Task restarter started");

                cancelCurrentTask();

                F0<Boolean> toRun = () -> {
                    try {
                        if (allowedStates.isEmpty() || allowedStates.get().contains(stateGetter.get())) {
                            //log.trace(() -> schedulerName + " - Executing task");
                            synchronized (mainTaskLock) {
                                O<M> apply = processor.apply();
                                //log.trace(() -> schedulerName + " - Task result:" + apply);
                                apply.ifPresent(resultPublisher::setIfNotNull);
                                //log.debug(() -> schedulerName + " - Run ok : " + apply.map($ -> $.toString()).orElse("O.empty
                                // ()"));
                            }
                            return true;
                        } else {
                            return false;
                        }
                    } catch (Exception e) {
                        //log.error(schedulerName, e);
                        throw e;
                    }
                };

                if (delayProvider.isStatic()) {
                    Long left = delayProvider.getDelay().apply();
                    //log.debug(() -> schedulerName + " - Reschedule executor for:" + left);
                    synchronized (auxiliaryLock) {
                        try {
                            currentTask = currentExecutor.scheduleWithFixedDelay(toRun::apply, left, left,
                                    TimeUnit.MILLISECONDS);
                        } catch (Exception e) {
                            //log.error("", e);
                            recreateExecutor(executor);
                        }
                    }
                } else {
                    //log.trace(() -> schedulerName + " - Reschedule executor for delay provider");
                    X1<R> reschedule = X.x(null);
                    reschedule.set(() -> {
                        synchronized (auxiliaryLock) {
                            Long apply = delayProvider.getDelay().apply();
                            //log.trace(() -> schedulerName + " - Delay provider for:" + apply + " " + this.hashCode());
                            long expectedId = expectedTaskId.incrementAndGet();
                            //log.trace(() -> schedulerName + " - ExpectedId:" + expectedId);
                            try {
                                currentTask = currentExecutor.schedule(() -> {
                                    Boolean apply1 = toRun.apply();

                                    synchronized (auxiliaryLock) {
                                        long toExpect = expectedTaskId.get();
                                        if (expectedId == toExpect) {
                                            reschedule.get().run();
                                        } else {
                                            //log.debug(() -> schedulerName + " - Unexpected:" + expectedId + "," + toExpect);
                                        }
                                    }
                                }, apply, TimeUnit.MILLISECONDS);
                            } catch (Exception e) {
                                //log.error("", e);
                                recreateExecutor(executor);
                            }
                        }
                    });
                    reschedule.get().run();
                }
            }
            //log.trace(() -> schedulerName + " - Task restarter finished");
        };
    }

    private void recreateExecutor(F0<ScheduledExecutorService> executorCreator) {
        if (currentExecutor != null) {
            //log.debug(() -> schedulerName + " oldExec" + System.identityHashCode(currentExecutor));
            currentExecutor.shutdownNow();
        }
        currentExecutor = executorCreator.apply();
        //log.debug(() -> schedulerName + " newExec" + System.identityHashCode(currentExecutor));

    }

    public void stop() {
        synchronized (auxiliaryLock) {
            if (started) {
                started = false;
                cancelCurrentTask();
            }
        }
    }

    public void start() {
        synchronized (auxiliaryLock) {
            if (!started) {
                //log.debug(() -> schedulerName + " - Started current task");
                started = true;
                taskRestarter.run();
            }
        }
    }


    public void restart() {
        synchronized (auxiliaryLock) {
            stop();
            start();
        }
    }

    private void cancelCurrentTask() {
        synchronized (auxiliaryLock) {
            if (currentTask != null) {
                //log.debug(() -> schedulerName + " - Cancel current task");
                currentTask.cancel(true);
            }
            currentTask = null;
            expectedTaskId.getAndUpdate(w -> w - Long.MAX_VALUE / 10);
        }
    }
}
