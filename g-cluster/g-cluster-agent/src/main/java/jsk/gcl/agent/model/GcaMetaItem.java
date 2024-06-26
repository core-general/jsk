package jsk.gcl.agent.model;

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

import sk.services.http.CrcAndSize;
import sk.utils.functional.O;
import sk.utils.statics.St;

import java.time.ZonedDateTime;

public record GcaMetaItem(String bucket, String pathInBucket, CrcAndSize crcAndSize, O<ZonedDateTime> updatedAt) {
    public GcaVersionId getVersionId() {
        return new GcaVersionId(St.subLL(pathInBucket, "/") + "_" + crcAndSize.getCrc32());
    }
}
