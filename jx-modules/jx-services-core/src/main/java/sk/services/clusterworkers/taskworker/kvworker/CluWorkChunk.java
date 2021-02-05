package sk.services.clusterworkers.taskworker.kvworker;

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

import lombok.EqualsAndHashCode;
import lombok.Value;
import sk.utils.async.cancel.CancelGetter;
import sk.utils.functional.F1;
import sk.utils.ids.IdBase;

@EqualsAndHashCode(callSuper = true)
@Value
public class CluWorkChunk<R> extends IdBase<String> {
    int badRetryCount;
    F1<CancelGetter, CluWorkChunkResult<R>> executor;

    public CluWorkChunk(String id, int badRetryCount,
            F1<CancelGetter, CluWorkChunkResult<R>> executor) {
        super(id);
        this.badRetryCount = badRetryCount;
        this.executor = executor;
    }
}
