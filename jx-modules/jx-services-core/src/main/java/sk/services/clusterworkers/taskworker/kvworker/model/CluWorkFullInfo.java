package sk.services.clusterworkers.taskworker.kvworker.model;

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
import lombok.RequiredArgsConstructor;
import sk.utils.functional.O;
import sk.utils.ifaces.Identifiable;
import sk.utils.statics.Cc;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
public class CluWorkFullInfo<T extends Identifiable<String>, R> {
    ArrayDeque<WorkPartInfo<T, R>> workActive = new ArrayDeque<>();
    Map<String, WorkPartInfo<T, R>> workLocked = Cc.m();
    //tasks in this list have restartAfter not empty to indicate that they have to wait some time before retry
    TreeMap<Long, List<WorkPartInfo<T, R>>> idleTasks = Cc.tm();

    List<WorkPartInfo<T, R>> workFail = Cc.l();
    List<WorkPartInfo<T, R>> workSuccess = Cc.l();

    @Data
    @RequiredArgsConstructor
    public static class WorkPartInfo<T extends Identifiable<String>, R> {
        final T workDescription;
        O<R> lastResult = O.empty();
        O<LockInfo> lock = O.empty();
        O<String> lastError = O.empty();
        int tryCount = 0;
    }

    @Data
    @AllArgsConstructor
    public static class LockInfo {
        String lockId;
        ZonedDateTime lockedAt;

        public boolean isObsolete(ZonedDateTime now, long maxLockLimitMs) {
            return getLockedAt().plus(maxLockLimitMs, ChronoUnit.MILLIS).isBefore(now);
        }
    }
}
