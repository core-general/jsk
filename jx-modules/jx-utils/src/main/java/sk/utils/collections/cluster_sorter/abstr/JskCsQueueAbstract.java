package sk.utils.collections.cluster_sorter.abstr;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2023 Core General
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

import sk.utils.collections.cluster_sorter.abstr.model.JskCsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsPollResult;
import sk.utils.statics.Cc;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface JskCsQueueAbstract<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE extends JskCsSource<SRC_ID, ITEM>> {

    void addAll(List<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> item);

    JskCsPollResult<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE> poll(EXPAND_DIRECTION direction);

    Map<EXPAND_DIRECTION, Iterator<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>>> getDirectionIterators();

    default public int calculateSize(Set<EXPAND_DIRECTION> directions) {
        return getDirectionIterators().entrySet().stream().mapToInt($ -> {
            return Cc.list($.getValue()).size();
        }).sum();
    }
}
