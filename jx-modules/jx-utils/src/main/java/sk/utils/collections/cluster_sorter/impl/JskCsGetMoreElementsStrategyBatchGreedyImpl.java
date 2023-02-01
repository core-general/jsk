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

import lombok.AllArgsConstructor;
import sk.utils.collections.cluster_sorter.JscCsBatchProcessor;
import sk.utils.collections.cluster_sorter.JskCsGetMoreElementsStrategy;
import sk.utils.collections.cluster_sorter.JskCsSource;
import sk.utils.collections.cluster_sorter.model.JskCsItem;
import sk.utils.collections.cluster_sorter.model.JskCsList;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class JskCsGetMoreElementsStrategyBatchGreedyImpl<SRC_ID, ITEM> implements JskCsGetMoreElementsStrategy<SRC_ID, ITEM> {
    private final JscCsBatchProcessor<SRC_ID, ITEM> batchProcessor;

    @Override
    public Map<SRC_ID, JskCsList<ITEM>> onNextLastItem(JskCsItem<SRC_ID, ITEM> itemToExpand,
            Iterator<JskCsItem<SRC_ID, ITEM>> sortedRestOfQueue, int itemsLeft) {
        int needItems = itemsLeft;
        //searching for expanding elements
        List<JskCsSource<SRC_ID, ITEM>> sourcesToExpand = Cc.l(itemToExpand.getSource());
        while (itemsLeft-- > 0 && sortedRestOfQueue.hasNext()) {
            final JskCsItem<SRC_ID, ITEM> queueItem = sortedRestOfQueue.next();
            if (queueItem.isExpandableLastItem()) {
                sourcesToExpand.add(queueItem.getSource());
            }
        }

        return batchProcessor.getNextElements(sourcesToExpand,
                sourcesToExpand.stream().map($ -> X.x($.getId(), needItems)).collect(Cc.toMX2()));
    }
}
