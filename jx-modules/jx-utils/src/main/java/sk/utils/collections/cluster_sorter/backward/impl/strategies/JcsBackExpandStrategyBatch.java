package sk.utils.collections.cluster_sorter.backward.impl.strategies;

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
import sk.utils.collections.cluster_sorter.abstr.model.JcsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JcsList;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSourceId;
import sk.utils.collections.cluster_sorter.backward.model.JcsEBackType;
import sk.utils.collections.cluster_sorter.backward.model.JcsIBackSource;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class JcsBackExpandStrategyBatch<ITEM, SOURCE extends JcsIBackSource<ITEM>>
        implements JcsIExpandElementsStrategy<ITEM, JcsEBackType, SOURCE> {
    private final JcsIBatchProcessor<ITEM, JcsEBackType, SOURCE> batchProcessor;

    @Override
    public Map<JcsSourceId, JcsList<ITEM>>
    getMoreFromSourceInDirection(SOURCE source,
            JcsEBackType direction,
            Iterator<JcsItem<ITEM, JcsEBackType, SOURCE>> sortedRestOfQueue,
            int itemsLeft) {

        int needItems = itemsLeft;
        List<SOURCE> sourcesToExpand = Cc.l(source);
        while (itemsLeft-- > 0 && sortedRestOfQueue.hasNext()) {
            final JcsItem<ITEM, JcsEBackType, SOURCE> queueItem = sortedRestOfQueue.next();
            if (queueItem.isExpandable()) {
                sourcesToExpand.add(queueItem.getSource());
            }
        }
        Map<JcsSourceId, Map<JcsEBackType, JcsList<ITEM>>> batchResult = batchProcessor.getNextElements(sourcesToExpand,
                sourcesToExpand.stream().map($ -> X.x($.getSourceId(), Cc.m(
                        direction, needItems
                ))).collect(Cc.toMX2()));


        return batchResult.entrySet().stream()
                .map($ -> X.x($.getKey(), $.getValue().get(direction)))
                .collect(Cc.toMX2());
    }
}
