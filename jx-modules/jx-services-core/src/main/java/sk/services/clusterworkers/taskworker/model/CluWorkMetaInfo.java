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

import lombok.*;
import sk.utils.collections.DequeWithLimit;
import sk.utils.functional.O;
import sk.utils.minmax.MinMaxAvgWithObj;

import java.time.ZonedDateTime;

@Data
@With
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class CluWorkMetaInfo<CUSTOM_META> {
    String runId;
    O<ZonedDateTime> startedAt = O.empty();
    O<ZonedDateTime> finishedAt = O.empty();
    Status status = Status.NOT_STARTED;
    long overallChunkCount;

    long lockedChunkCount;
    long idleChunkCount;

    long currentFailCount;
    long finalDoneChunkCount;
    long finalFailChunkCount;

    O<Long> lastBackoffIdleWait = O.empty();

    MinMaxAvgWithObj<String> retryCounts = new MinMaxAvgWithObj<>();
    O<ZonedDateTime> lastMetaCountUpdate = O.empty();
    O<String> failMessage = O.empty();
    O<CUSTOM_META> additionalInfo = O.empty();

    DequeWithLimit<CluWorkMetaInfo<CUSTOM_META>> deque = new DequeWithLimit<>(5);

    public boolean isWorkActive() {
        return status == Status.STARTED;
    }

    public boolean isFinished() {
        return status != Status.STARTED && status != Status.NOT_STARTED;
    }

    /**
     * We take with locks, to check if locks are still valid
     *
     * @return
     */
    public long getReadyToBeProcessedChunkCountWithlLocks() {
        return overallChunkCount - finalDoneChunkCount - finalFailChunkCount;
    }

    public DequeWithLimit<CluWorkMetaInfo<CUSTOM_META>> getDeque() {
        if (deque == null) {
            deque = new DequeWithLimit<>(5);
        }
        return deque;
    }

    public void restart(String newRunId, ZonedDateTime now, int overallChunkCount, O<CUSTOM_META> additionalInfo) {
        if (runId != null) {
            getDeque().addFirst(this.withDeque(null));
        }
        runId = newRunId;
        startedAt = O.of(now);
        finishedAt = O.empty();
        status = Status.STARTED;
        this.overallChunkCount = overallChunkCount;
        lockedChunkCount = 0;
        currentFailCount = 0;
        idleChunkCount = 0;

        finalDoneChunkCount = 0;
        finalFailChunkCount = 0;
        retryCounts = new MinMaxAvgWithObj<>();
        failMessage = O.empty();
        lastBackoffIdleWait = O.empty();
        this.additionalInfo = additionalInfo;
    }

    @SuppressWarnings("unused")
    public enum Status {
        NOT_STARTED, STARTED, FINISHED, FAILED
    }
}
