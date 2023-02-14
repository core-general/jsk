package sk.utils.collections.cluster_sorter.backward.impl;

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

import sk.utils.collections.cluster_sorter.abstr.JcsASorter;
import sk.utils.collections.cluster_sorter.abstr.JcsIExpandElementsStrategy;
import sk.utils.collections.cluster_sorter.abstr.JcsInitStrategy;
import sk.utils.collections.cluster_sorter.abstr.model.JcsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JcsList;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSources;
import sk.utils.collections.cluster_sorter.backward.JcsIQueueBack;
import sk.utils.collections.cluster_sorter.backward.JcsISorterBack;
import sk.utils.collections.cluster_sorter.backward.impl.strategies.*;
import sk.utils.collections.cluster_sorter.backward.model.JcsEBackType;
import sk.utils.collections.cluster_sorter.backward.model.JcsIBackSource;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;

import java.util.*;

/**
 * We cache values which we already polled and return them back if we go back
 */
public class JcsSorterBack<ITEM, SOURCE extends JcsIBackSource<ITEM>>
        extends JcsASorter<ITEM, JcsEBackType, JcsIQueueBack<ITEM, SOURCE>, SOURCE>
        implements JcsISorterBack<ITEM, SOURCE> {

    public static <ITEM, SOURCE extends JcsIBackSource<ITEM>> JcsSorterBack<ITEM, SOURCE> custom(
            List<SOURCE> sources,
            Comparator<ITEM> comparator,
            JcsInitStrategy<ITEM, JcsEBackType, SOURCE> firstFeedStrategy,
            JcsIExpandElementsStrategy<ITEM, JcsEBackType, SOURCE> getMoreStrategy) {
        return new JcsSorterBack<>(sources, comparator, firstFeedStrategy, getMoreStrategy);
    }

    public static <ITEM, SOURCE extends JcsIBackSource<ITEM>> JcsSorterBack<ITEM, SOURCE> simple(
            List<SOURCE> sources,
            Comparator<ITEM> comparator) {
        return new JcsSorterBack<>(sources, comparator, new JcsBackInitStrategySimple<>(), new JcsBackExpandStrategySimple<>());
    }

    public static <ITEM, SOURCE extends JcsIBackSource<ITEM>> JcsSorterBack<ITEM, SOURCE> batch(
            List<SOURCE> sources,
            Comparator<ITEM> comparator, JcsIBackBatch<ITEM, SOURCE> batchProcessor) {
        return new JcsSorterBack<>(sources, comparator, new JcsBackInitStrategyBatch<>(batchProcessor),
                new JcsBackExpandStrategyBatch<>(batchProcessor));
    }

    private JcsSorterBack(
            List<SOURCE> sources,
            Comparator<ITEM> comparator,
            JcsInitStrategy<ITEM, JcsEBackType, SOURCE> firstFeedStrategy,
            JcsIExpandElementsStrategy<ITEM, JcsEBackType, SOURCE> getMoreStrategy
    ) {
        super(sources, comparator, firstFeedStrategy, getMoreStrategy);
    }

    @Override
    protected JcsIQueueBack<ITEM, SOURCE> instantiateQueue() {
        return new JcsQueueBack<>();
    }

    @Override
    protected void addNewSourcePrivate(SOURCE source, Map<JcsEBackType, JcsList<ITEM>> initialSourceItems, O<ITEM> position) {
        addNewSourcePrivate0(source, new JcsSources<>(Cc.l(source)), initialSourceItems, 1, position);
    }

    private void addNewSourcePrivate0(SOURCE source, JcsSources<ITEM, SOURCE> sources,
            Map<JcsEBackType, JcsList<ITEM>> initialSourceItems, int iteration, O<ITEM> position) {
        processSourceRequestResult(Cc.m(source.getId(), initialSourceItems), sources, position);
        boolean processOtherQueue = true;
        if (O.ofNull(initialSourceItems.get(JcsEBackType.FORWARD)).map($ -> $.isHasMoreElements()).orElse(false)) {
            processOtherQueue =
                    addNewSourcePrivateOneQueue(source, sources, queue.iteratorBack(), JcsEBackType.FORWARD, iteration,
                            position);
        }
        if (processOtherQueue &&
                O.ofNull(initialSourceItems.get(JcsEBackType.BACKWARD)).map($ -> $.isHasMoreElements()).orElse(false)) {
            addNewSourcePrivateOneQueue(source, sources, queue.iterator(), JcsEBackType.BACKWARD, iteration, position);
        }
    }

    /**
     * @param queueItems we are checking particular queue to understand if there are bad elements inside. If there are -
     *                   we try to push them to other queue iteratively
     * @param badType    type of expand which is not allowed in the particular part of the queue
     * @param iteration  number of iteration, which influences how many items we request from sources
     * @param position
     * @return - true if other queue should be processed
     */
    private boolean addNewSourcePrivateOneQueue(
            SOURCE source,
            JcsSources<ITEM, SOURCE> sources,
            Iterator<JcsItem<ITEM, JcsEBackType, SOURCE>> queueItems,
            JcsEBackType badType,
            int iteration, O<ITEM> position) {
        boolean hasExpandableItemOfGoodType = false;
        JcsList<ITEM> items = null;
        for (JcsItem<ITEM, JcsEBackType, SOURCE> item : Cc.iterable(queueItems)) {
            if (item.isExpandable() && Fu.equal(item.getSource().getId(), source.getId())) {
                if (item.getExpandDirection() == badType) {
                    items = getMoreFromSourceStrategy.getMoreFromSourceInDirection(source, badType, Collections.emptyIterator(),
                            Math.min(1000, iteration * iteration * iteration * 10)).get(source.getId());
                } else {
                    hasExpandableItemOfGoodType = true;
                }
            }
        }
        if (items != null) {
            addNewSourcePrivate0(source, sources, Cc.m(badType, items), iteration + 1, position);
        }
        return !hasExpandableItemOfGoodType;
    }
}
