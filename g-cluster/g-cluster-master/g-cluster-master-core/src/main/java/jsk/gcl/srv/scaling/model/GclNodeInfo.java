package jsk.gcl.srv.scaling.model;

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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sk.utils.collections.DequeWithLimit;
import sk.utils.functional.O;

import java.time.ZonedDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GclNodeInfo {
    public static final String type = "jsk.gcl.srv.scaling.model.GclNodeInfo";

    ZonedDateTime nodeStart;
    int minWorkers;
    int curMaxWorkers;
    int overallMaxWorkers;
    GclAvgStats lastMinPerWorkerAvgStat;
    GclAvgStats lastMinNodeAvgStat;
    GclAvgStats fullHistPerWorkerAvgStat;
    GclAvgStats fullHistNodeAvgStat;
    DequeWithLimit<GclNodeHistoryItem> history;

    OOMInfo oom;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OOMInfo {
        O<ZonedDateTime> lastMaxChange;
        DequeWithLimit<OOMFact> facts;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OOMFact {
        ZonedDateTime oomDate;
        int curWorkerCount;
    }
}
