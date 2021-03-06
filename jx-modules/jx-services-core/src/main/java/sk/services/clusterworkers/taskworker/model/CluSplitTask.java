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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sk.services.clusterworkers.taskworker.kvworker.CluWorkChunk;
import sk.services.clusterworkers.taskworker.kvworker.CluWorkChunkResult;
import sk.utils.functional.C1;
import sk.utils.functional.O;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CluSplitTask<CUSTOM_META, RESULT> {
    CluWorkMetaInfo<CUSTOM_META> metaInfo;
    O<List<CluWorkChunk<RESULT>>> tasksToProcess = O.empty();
    O<CluAsyncTaskExecutor<RESULT>> executor = O.empty();
    O<C1<O<List<CluWorkChunkResult<RESULT>>>>> finisher = O.empty();

    public CluSplitTask(CluWorkMetaInfo<CUSTOM_META> metaInfo) {
        this.metaInfo = metaInfo;
    }

    public void finishTasks(O<List<CluWorkChunkResult<RESULT>>> applyResults) {
        finisher.ifPresent($ -> $.accept(applyResults));
    }
}
