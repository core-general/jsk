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
import sk.utils.collections.cluster_sorter.abstr.model.JskCsSrcId;
import sk.utils.collections.cluster_sorter.forward.model.JskCsForwardType;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class JskCsForwardExpandBatchGreedyImpl<
        ITEM,
        SOURCE extends JskCsSource<ITEM>
        >
        implements JskCsExpandElementsStrategy<ITEM, JskCsForwardType, SOURCE> {
    private final JskCsBatchProcessor<ITEM, JskCsForwardType> batchProcessor;

    @Override
    public Map<JskCsSrcId, Map<JskCsForwardType, JskCsList<ITEM>>> onNextLastItem(
            JskCsItem<ITEM, JskCsForwardType, SOURCE> itemToExpand,
            Map<JskCsForwardType, Iterator<JskCsItem<ITEM, JskCsForwardType, SOURCE>>> sortedRestOfQueuePaths,
            int itemsLeft) {

        int needItems = itemsLeft;
        List<JskCsSource<ITEM>> sourcesToExpand = Cc.l(itemToExpand.getSource());
        Iterator<JskCsItem<ITEM, JskCsForwardType, SOURCE>> sortedRestOfQueue =
                sortedRestOfQueuePaths.get(JskCsForwardType.FORWARD);
        while (itemsLeft-- > 0 && sortedRestOfQueue.hasNext()) {
            final JskCsItem<ITEM, JskCsForwardType, SOURCE> queueItem = sortedRestOfQueue.next();
            if (queueItem.isExpandable()) {
                sourcesToExpand.add(queueItem.getSource());
            }
        }
        return batchProcessor.getNextElements(sourcesToExpand,
                sourcesToExpand.stream().map($ -> X.x($.getId(), Cc.m(JskCsForwardType.FORWARD, needItems))).collect(Cc.toMX2()));
    }
}
