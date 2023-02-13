package sk.utils.collections.cluster_sorter.forward.impl.strategies;

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

import sk.utils.collections.cluster_sorter.abstr.JskCsExpandElementsStrategy;
import sk.utils.collections.cluster_sorter.abstr.JskCsSource;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsList;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsSrcId;
import sk.utils.collections.cluster_sorter.forward.model.JskCsForwardType;
import sk.utils.statics.Cc;

import java.util.Iterator;
import java.util.Map;

public class JskCsForwardExpandElementSimpleImpl<ITEM, SOURCE extends JskCsSource<ITEM>>
        implements JskCsExpandElementsStrategy<ITEM, JskCsForwardType, SOURCE> {

    @Override
    public Map<JskCsSrcId, Map<JskCsForwardType, JskCsList<ITEM>>> onNextLastItem(
            JskCsItem<ITEM, JskCsForwardType, SOURCE> itemToExpand,
            Map<JskCsForwardType, Iterator<JskCsItem<ITEM, JskCsForwardType, SOURCE>>> sortedRestOfQueuePaths,
            int itemsLeft) {
        return Cc.m(itemToExpand.getSource().getId(),
                Cc.m(JskCsForwardType.FORWARD, itemToExpand.getSource().getNextElements(itemsLeft)));
    }
}
