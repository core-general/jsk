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
import sk.utils.collections.cluster_sorter.abstr.JcsIBatchProcessor;
import sk.utils.collections.cluster_sorter.abstr.JcsIExpandElementsStrategy;
import sk.utils.collections.cluster_sorter.abstr.JcsISource;
import sk.utils.collections.cluster_sorter.abstr.model.JcsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JcsList;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSrcId;
import sk.utils.collections.cluster_sorter.forward.model.JcsEForwardType;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class JcsForwardBatchExpandGreedy<
        ITEM,
        SOURCE extends JcsISource<ITEM>
        >
        implements JcsIExpandElementsStrategy<ITEM, JcsEForwardType, SOURCE> {
    private final JcsIBatchProcessor<ITEM, JcsEForwardType> batchProcessor;

    @Override
    public Map<JcsSrcId, Map<JcsEForwardType, JcsList<ITEM>>> onNextLastItem(
            JcsItem<ITEM, JcsEForwardType, SOURCE> itemToExpand,
            Map<JcsEForwardType, Iterator<JcsItem<ITEM, JcsEForwardType, SOURCE>>> sortedRestOfQueuePaths,
            int itemsLeft) {

        int needItems = itemsLeft;
        List<JcsISource<ITEM>> sourcesToExpand = Cc.l(itemToExpand.getSource());
        Iterator<JcsItem<ITEM, JcsEForwardType, SOURCE>> sortedRestOfQueue =
                sortedRestOfQueuePaths.get(JcsEForwardType.FORWARD);
        while (itemsLeft-- > 0 && sortedRestOfQueue.hasNext()) {
            final JcsItem<ITEM, JcsEForwardType, SOURCE> queueItem = sortedRestOfQueue.next();
            if (queueItem.isExpandable()) {
                sourcesToExpand.add(queueItem.getSource());
            }
        }
        return batchProcessor.getNextElements(sourcesToExpand,
                sourcesToExpand.stream().map($ -> X.x($.getId(), Cc.m(JcsEForwardType.FORWARD, needItems))).collect(Cc.toMX2()));
    }
}
