package sk.utils.collections.cluster_sorter.impl;

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

import sk.utils.collections.cluster_sorter.JskCsGetMoreElementsStrategy;
import sk.utils.collections.cluster_sorter.model.JskCsItem;
import sk.utils.collections.cluster_sorter.model.JskCsList;
import sk.utils.statics.Cc;

import java.util.Iterator;
import java.util.Map;

public class JskCsGetMoreElementsStrategySimpleImpl<SRC_ID, ITEM>
        implements JskCsGetMoreElementsStrategy<SRC_ID, ITEM> {

    @Override
    public Map<SRC_ID, JskCsList<ITEM>> onNextLastItem(
            JskCsItem<SRC_ID, ITEM> itemToExpand,
            Iterator<JskCsItem<SRC_ID, ITEM>> sortedRestOfQueue, int itemsLeft
    ) {
        return Cc.m(itemToExpand.getSource().getId(), itemToExpand.getSource().getNextElements(itemsLeft));
    }
}