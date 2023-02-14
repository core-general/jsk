package sk.utils.collections.cluster_sorter.backward;

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

import sk.utils.collections.cluster_sorter.abstr.JcsISorter;
import sk.utils.collections.cluster_sorter.backward.model.JcsEBackType;
import sk.utils.collections.cluster_sorter.backward.model.JcsIBackSource;

import java.util.List;

public interface JcsISorterBack<ITEM, SOURCE extends JcsIBackSource<ITEM>>
        extends JcsISorter<ITEM, JcsEBackType, SOURCE> {
    default List<ITEM> getNext(int count) {
        return getNext(count, JcsEBackType.FORWARD);
    }

    default boolean hasNext(int initializingCount) {
        return hasNext(initializingCount, JcsEBackType.FORWARD);
    }

    default List<ITEM> getPrevious(int count) {
        return getNext(count, JcsEBackType.BACKWARD);
    }

    default boolean hasPrevious(int count) {
        return hasNext(count, JcsEBackType.BACKWARD);
    }
}
