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

import sk.utils.collections.cluster_sorter.abstr.model.JcsSrcId;

import java.util.List;
import java.util.Map;

public interface JcsISorter<ITEM, EXPAND_DIRECTION, SOURCE extends JcsISource<ITEM>> {
    List<ITEM> getNext(int count, EXPAND_DIRECTION direction);

    boolean hasNext(int initializingCount, EXPAND_DIRECTION direction);

    void setPositionToItem(ITEM item);

    Map<JcsSrcId, SOURCE> getAllSources();

    void addNewSource(SOURCE source);

    List<ITEM> removeSource(JcsSrcId id);
}
