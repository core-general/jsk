package jsk.gcl.srv.scaling.workers;

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
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class GclScalingLocalWorkerManager {
    @Getter
    @Setter
    private volatile int desiredLocalWorkerCount;

    private final ConcurrentHashMap<String, GclWorker> workers = new ConcurrentHashMap<>();
    private final ArrayBlockingQueue<GclJobDto<?, ?>> jobs = new ArrayBlockingQueue<>(1000, true);
    private final ConcurrentHashMap<String, GclJobDto<?, ?>> inWorkJobs = new ConcurrentHashMap<>();

    public int getLocalWorkerCount() {
        return workers.size();
    }


    public int getBufferJobCount() {
        //todo
        return -1;
    }
}
