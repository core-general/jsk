package jsk.gcl.srv.logic.scaling.workers;

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

import jsk.gcl.srv.logic.jobs.services.GclJobManager;
import jsk.gcl.srv.logic.scaling.GclOOMManager;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import sk.services.ids.IIds;
import sk.utils.functional.O;
import sk.utils.statics.Fu;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class GclScalingLocalWorkerManager {
    @Getter
    private volatile int desiredLocalWorkerCount;
    private final ConcurrentHashMap<String, GclWorker> workers = new ConcurrentHashMap<>();

    @Inject GclOOMManager oomMgr;
    @Inject GclJobManager jobManager;
    @Inject IIds ids;

    public int getLocalWorkerCount() {
        return workers.size();
    }

    public void setDesiredLocalWorkerCount(int desiredLocalWorkerCount) {
        this.desiredLocalWorkerCount = desiredLocalWorkerCount;
        if (desiredLocalWorkerCount > getLocalWorkerCount()) {
            synchronized (GclScalingLocalWorkerManager.this) {
                desiredLocalWorkerCount = this.desiredLocalWorkerCount;
                if (desiredLocalWorkerCount > getLocalWorkerCount()) {
                    final int numToStart = desiredLocalWorkerCount - getLocalWorkerCount();
                    startNewWorkers(numToStart);
                }
            }
        }
    }

    /***
     * We add workers on change of set desired worker count.
     * We remove workers on each worker's iteration after comparing with current desired task
     */
    private void startNewWorkers(int numToStart) {
        Fu.run(numToStart, () -> {
            String workerId = ids.shortIdS();
            GclWorker worker = new GclWorker(
                    jobManager::waitForTask,
                    oe -> onWorkerIterationFinish(workerId, oe),
                    oomMgr);
            workers.put(workerId, worker);
        });
    }

    @NotNull
    private Boolean onWorkerIterationFinish(String workerId, O<Exception> oe) {
        oe.ifPresent(e -> log.error("", e));
        if (desiredLocalWorkerCount < getLocalWorkerCount()) {
            synchronized (GclScalingLocalWorkerManager.this) {
                if (desiredLocalWorkerCount < getLocalWorkerCount()) {
                    workers.remove(workerId);
                    return false;
                }
            }
        }
        return true;
    }
}
