package sk.services.clusterworkers.taskworker.model;

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

import sk.services.clusterworkers.taskworker.kvworker.CluWorkChunkResult;
import sk.services.retry.utils.BatchRepeatResult;
import sk.services.retry.utils.IdCallable;
import sk.services.retry.utils.QueuedTask;

import java.util.Map;

public class CluTaskBatchResult<RESULT>
        extends BatchRepeatResult<String, CluWorkChunkResult<RESULT>, IdCallable<String, CluWorkChunkResult<RESULT>>> {
    public CluTaskBatchResult(
            Map<String, QueuedTask<String, CluWorkChunkResult<RESULT>, IdCallable<String, CluWorkChunkResult<RESULT>>>> result) {
        super(result);
    }
}
