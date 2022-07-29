package sk.outer.graph;

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

import sk.exceptions.NotImplementedException;
import sk.outer.graph.execution.MgcGraphHistoryItem;
import sk.outer.graph.execution.MgcObjectType;
import sk.utils.functional.O;
import sk.utils.paging.SimplePage;

import java.util.List;

public interface MgcHistoryProvider {
    void addGraphHistoryItem(MgcGraphHistoryItem item);

    SimplePage<MgcGraphHistoryItem, String> getHistory(int count, O<String> npa,
            boolean ascending, MgcObjectType type);

    boolean hasHistory();

    default void clearHistory() {
        throw new NotImplementedException();
    }

    default void removeHistoryItem(MgcGraphHistoryItem item) {
        throw new NotImplementedException();
    }

    default List<MgcGraphHistoryItem> getHistoryDescending(int lastX) {
        return getHistory(lastX, O.empty(), false, MgcObjectType.BOTH).getData();
    }

    default List<MgcGraphHistoryItem> getHistoryDescending(int lastX, MgcObjectType type) {
        return getHistory(lastX, O.empty(), false, type).getData();
    }

    default O<MgcGraphHistoryItem> getLastNode() {
        List<MgcGraphHistoryItem> data = getHistory(1, O.empty(), false, MgcObjectType.NODE).getData();
        if (data.size() == 0 || !data.get(0).isNode()) {
            return O.empty();
        } else {
            return O.of(data.get(0));
        }
    }
}
