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

import sk.utils.collections.cluster_sorter.abstr.model.JcsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JcsPollResult;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface JcsIQueue<ITEM, EXPAND_DIRECTION, SOURCE extends JcsISource<ITEM>> {
    /** If items were already consumed, added elemen */
    void addAllRespectConsumed(List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> items);

    O<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> getLastConsumedItem();

    JcsPollResult<ITEM, EXPAND_DIRECTION, SOURCE> poll(EXPAND_DIRECTION direction);

    Map<EXPAND_DIRECTION, Iterator<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>>> getDirectionIterators();

    default public int calculateSize(Set<EXPAND_DIRECTION> directions) {
        return getDirectionIterators().entrySet().stream().mapToInt($ -> {
            return Cc.list($.getValue()).size();
        }).sum();
    }

    default void onDidNotGetToMainQueueWhenAddRespectOrder(List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> items) {}

    void clear();
}
