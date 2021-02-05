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

import lombok.Data;
import sk.utils.ifaces.Identifiable;
import sk.utils.statics.Cc;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
class CluWorkFullInfo<T extends Identifiable<String>, R> {
    ArrayDeque<CluKvSplitWorkPartInfo<T, R>> workActive = new ArrayDeque<>();
    Map<String, CluKvSplitWorkPartInfo<T, R>> workLocked = Cc.m();
    //tasks in this list have restartAfter not empty to indicate that they have to wait some time before retry
    TreeMap<Long, List<CluKvSplitWorkPartInfo<T, R>>> idleTasks = Cc.tm();

    List<CluKvSplitWorkPartInfo<T, R>> workFail = Cc.l();
    List<CluKvSplitWorkPartInfo<T, R>> workSuccess = Cc.l();
}
