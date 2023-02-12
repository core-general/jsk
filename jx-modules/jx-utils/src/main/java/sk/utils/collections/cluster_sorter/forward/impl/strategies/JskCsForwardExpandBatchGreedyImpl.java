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

import lombok.AllArgsConstructor;
import sk.utils.collections.cluster_sorter.abstr.JskCsBatchProcessor;
import sk.utils.collections.cluster_sorter.abstr.JskCsExpandElementsStrategy;
import sk.utils.collections.cluster_sorter.abstr.JskCsSource;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsList;
import sk.utils.collections.cluster_sorter.forward.model.JskCsForwardType;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class JskCsForwardExpandBatchGreedyImpl<
        SRC_ID,
        ITEM,
        SOURCE extends JskCsSource<SRC_ID, ITEM>
        >
        implements JskCsExpandElementsStrategy<SRC_ID, ITEM, JskCsForwardType, SOURCE> {
    private final JskCsBatchProcessor<SRC_ID, ITEM, JskCsForwardType> batchProcessor;

    @Override
    public Map<SRC_ID, Map<JskCsForwardType, JskCsList<ITEM>>> onNextLastItem(
            JskCsItem<SRC_ID, ITEM, JskCsForwardType, SOURCE> itemToExpand,
            Map<JskCsForwardType, Iterator<JskCsItem<SRC_ID, ITEM, JskCsForwardType, SOURCE>>> sortedRestOfQueuePaths,
            int itemsLeft) {

        int needItems = itemsLeft;
        List<JskCsSource<SRC_ID, ITEM>> sourcesToExpand = Cc.l(itemToExpand.getSource());
        Iterator<JskCsItem<SRC_ID, ITEM, JskCsForwardType, SOURCE>> sortedRestOfQueue =
                sortedRestOfQueuePaths.get(JskCsForwardType.FORWARD);
        while (itemsLeft-- > 0 && sortedRestOfQueue.hasNext()) {
            final JskCsItem<SRC_ID, ITEM, JskCsForwardType, SOURCE> queueItem = sortedRestOfQueue.next();
            if (queueItem.isExpandable()) {
                sourcesToExpand.add(queueItem.getSource());
            }
        }
        return batchProcessor.getNextElements(sourcesToExpand,
                sourcesToExpand.stream().map($ -> X.x($.getId(), Cc.m(JskCsForwardType.FORWARD, needItems))).collect(Cc.toMX2()));
    }
}
