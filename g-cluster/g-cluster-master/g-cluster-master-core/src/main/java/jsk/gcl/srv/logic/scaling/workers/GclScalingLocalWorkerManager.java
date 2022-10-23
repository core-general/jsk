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

import jsk.gcl.srv.jpa.GclJobId;
import jsk.gcl.srv.logic.jobs.services.GclJobManager;
import lombok.Getter;
import lombok.Setter;
import sk.exceptions.NotImplementedException;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GclScalingLocalWorkerManager {
    @Getter
    @Setter
    private volatile int desiredLocalWorkerCount;
    private final ConcurrentHashMap<String, GclWorker> workers = new ConcurrentHashMap<>();

    @Inject GclJobManager jobManager;


    public int getLocalWorkerCount() {
        return workers.size();
    }


    public int getBufferJobCount() {
        throw new NotImplementedException();//todo
    }

    public List<GclJobId> getAllInProgressJobIds() {

    }
}
