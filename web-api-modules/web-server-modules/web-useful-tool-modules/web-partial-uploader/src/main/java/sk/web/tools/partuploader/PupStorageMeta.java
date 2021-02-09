package sk.web.tools.partuploader;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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
import sk.exceptions.JskProblem;
import sk.exceptions.JskProblemException;
import sk.utils.functional.O;
import sk.utils.tuples.X;
import sk.web.tools.partuploader.api.PupEUploadStatus;
import sk.web.tools.partuploader.api.PupMUploadMeta;
import sk.web.tools.partuploader.api.PupMUploadStatus;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PupStorageMeta<META, FINISH> {
    PupMUploadStatus<META, FINISH> status = new PupMUploadStatus<>();
    Map<Long, PupMUploadMeta<META>> parts = new HashMap<>();

    public PupStorageMeta<META, FINISH> exception(PupExc orElse, String info, PupMUploadMeta<META> partMeta, ZonedDateTime now) {
        status.setStatus(PupEUploadStatus.FAILED);
        status.setUploadFinish(O.of(now));
        status.setFirstFail(O.of(X.x(orElse, info, partMeta)));
        return this;
    }

    public PupStorageMeta<META, FINISH> processPart(PupMUploadMeta<META> partMeta, ZonedDateTime now) {
        if (parts.containsKey(partMeta.getPartId().getCurrentPart())) {
            throw new JskProblemException(JskProblem.code(PupExc.PUP_PART_ALREADY_PROCESSED), false);
        }
        parts.put(partMeta.getPartId().getCurrentPart(), partMeta);
        status.update(partMeta, now);
        return this;
    }
}
