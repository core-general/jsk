package jsk.gcl.srv.logic.jobs.services;

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

import jsk.gcl.cli.model.GclJobDto;
import jsk.gcl.cli.model.GclJobId;
import jsk.gcl.cli.model.GclJobType;
import jsk.gcl.srv.GclAppProperties;
import jsk.gcl.srv.jpa.GclJob;
import jsk.gcl.srv.logic.jobs.model.GclJobStatus;
import jsk.gcl.srv.logic.jobs.storage.GclJobStorage;
import sk.utils.async.locks.JReadWriteLock;
import sk.utils.async.locks.JReadWriteLockDecorator;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.R;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GclJobManager {
    private ArrayBlockingQueue<GclJobDto<?, ?, ?>> freeJobs;
    private ConcurrentHashMap<GclJobId, GclJobDto<?, ?, ?>> inProgressJobs;

    private final JReadWriteLock rwl = new JReadWriteLockDecorator(new ReentrantReadWriteLock(true));

    @Inject GclAppProperties appProps;
    @Inject GclJobStorage jobs;
    @Inject GclJobPayloadManager payloadManager;

    @PostConstruct
    public void init() {
        rwl.writeLock().runInLock(() -> {
            if (freeJobs == null) {
                freeJobs = new ArrayBlockingQueue<>(1000, true);
                inProgressJobs = new ConcurrentHashMap<>();
            }
        });
    }

    public List<GclJobId> getAllNodeJobs() {
        return rwl.readLock().getInLock(() -> {
            return Cc.addStream(freeJobs.stream().map($ -> $.getJobId()), inProgressJobs.keySet().stream()).distinct().toList();
        });
    }

    public int getBufferJobCount() {
        return freeJobs.size();
    }

    public <IN_CLS, OUT_CLS, T extends GclJobType<IN_CLS, OUT_CLS>> O<R> waitForTask() {
        final O<GclJobDto<IN_CLS, OUT_CLS, T>> oJob = tryAcquireJob();
        return oJob.map(job -> {
            final F1<IN_CLS, OUT_CLS> jobRunner = payloadManager.getJobRunner(job.getJobType());

            return () -> {
                jobs.transactionWithSaveX1(() -> {
                    final O<GclJob> ogclJob = jobs.getJobById(job.getJobId());
                    if (ogclJob.isEmpty()) {
                        return X.x(O.empty());
                    }

                    final GclJob gclJob = ogclJob.get();
                    if (
                            gclJob.getJStatus() == GclJobStatus.IN_FLIGHT
                    ) {
                        try {
                            final OUT_CLS out = jobRunner.apply(job.getParams());
                            gclJob.succeed(out);
                            jobs.incrementJobGroupSuccess(gclJob.getJJgId());
                        } catch (Exception e) {
                            boolean incrementJobGroupFail =
                                    gclJob.failAndChangeJobGroup(e, appProps.getMaxTaskRetriesAfterFails());
                            if (incrementJobGroupFail) {
                                jobs.incrementJobGroupFail(gclJob.getJJgId());
                            }
                        }
                        return X.x(ogclJob);
                    } else {
                        return X.x(O.empty());
                    }
                });
                //we remove anyway, in case of success is trivial, in case of fail we want to retry on the other node
                removeJobFromInProgress(job.getJobId());
            };
        });
    }


    private void removeJobFromInProgress(GclJobId id) {
        rwl.writeLock().runInLock(() -> {
            inProgressJobs.remove(id);
        });
    }

    private void addJobs(List<GclJobDto<?, ?, ?>> jobs) {
        rwl.writeLock().runInLock(() -> {
            freeJobs.addAll(jobs);
        });
    }

    @SuppressWarnings("unchecked")
    private <IN_CLS, OUT_CLS, T extends GclJobType<IN_CLS, OUT_CLS>> O<GclJobDto<IN_CLS, OUT_CLS, T>> tryAcquireJob() {
        return (O<GclJobDto<IN_CLS, OUT_CLS, T>>) rwl.writeLock().getIfLockFree(() -> {
            try {
                final GclJobDto<?, ?, ?> job = freeJobs.poll(2500, TimeUnit.MILLISECONDS);
                if (jobs == null) {
                    return O.<GclJobDto<?, ?, ?>>empty();
                }

                inProgressJobs.put(job.getJobId(), job);
                return O.of(job);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, Duration.ofMillis(2500)).flatMap($ -> $);
    }
}
