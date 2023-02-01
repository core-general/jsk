package sk.utils.collections.cluster_sorter;

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

import sk.utils.collections.cluster_sorter.model.JskCsItem;
import sk.utils.functional.O;

import java.util.Iterator;
import java.util.List;

public interface JskCsQueue<SRC_ID, ITEM> extends Iterable<JskCsItem<SRC_ID, ITEM>> {
    void addAll(List<JskCsItem<SRC_ID, ITEM>> item);

    O<JskCsItem<SRC_ID, ITEM>> poll();

    Iterator<JskCsItem<SRC_ID, ITEM>> iterator();

    default public int calculateSize() {
        int i = 0;
        for (JskCsItem<SRC_ID, ITEM> item : this) {
            i++;
        }
        return i;
    }
}
