package sk.web.tools.partuploader.api;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sk.exceptions.JskProblem;
import sk.exceptions.JskProblemException;
import sk.utils.functional.O;
import sk.utils.tuples.X3;
import sk.web.tools.partuploader.PupExc;

import java.time.ZonedDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PupMUploadStatus<META, FINISH> {
    PupMUploadId id;
    long finishedParts;
    long uploadedSize;
    long maxParts;
    long maxSize;
    PupEUploadStatus status;
    ZonedDateTime uploadStart;
    O<ZonedDateTime> uploadFinish = O.empty();
    O<FINISH> uploadFinishMeta = O.empty();
    O<X3<PupExc, String, PupMUploadMeta<META>>> firstFail = O.empty();

    public void update(PupMUploadMeta<META> partMeta, ZonedDateTime now) {
        if (id == null) {
            id = partMeta.getPartId().getId();
        }
        finishedParts++;
        uploadedSize += partMeta.getCurrentSize();
        if (maxParts == 0) {
            maxParts = partMeta.getMaxParts();
        }
        if (maxSize == 0) {
            maxSize = partMeta.getOverallSize();
        }
        if (status == null) {
            status = PupEUploadStatus.IN_PROCESS;
        }
        if (uploadStart == null) {
            uploadStart = now;
        }
        checkFinish(now);
    }

    private void checkFinish(ZonedDateTime now) {
        if (finishedParts == maxParts) {
            if (uploadedSize != maxSize) {
                throw new JskProblemException(JskProblem.code(PupExc.PUP_FINISH_SIZE_WRONG), false);
            }
            status = PupEUploadStatus.PRE_FINISHED;
        }
    }
}
