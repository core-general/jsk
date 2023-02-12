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

import lombok.Getter;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsList;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsPollResult;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsSources;
import sk.utils.functional.F0;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * !!!NOT THREAD SAFE!!!
 *
 * @param <SRC_ID>
 * @param <ITEM>
 */
public abstract class JskCsAbstractImpl<
        SRC_ID,
        ITEM,
        EXPAND_DIRECTION,
        QUEUE extends JskCsQueueAbstract<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>,
        SOURCE extends JskCsSource<SRC_ID, ITEM>
        >
        implements JskCsAbstract<SRC_ID, ITEM, EXPAND_DIRECTION> {
    protected final JskCsSources<SRC_ID, ITEM, SOURCE> sources;
    protected final Comparator<ITEM> comparator;
    protected final JskCsInitStrategy<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE> firstFeedStrategy;
    protected final JskCsExpandElementsStrategy<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE> getMoreStrategy;

    @Getter protected final QUEUE queue = instantiateQueue();

    protected final Map<SRC_ID, JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> expandableLastItems = Cc.m();
    protected boolean initDone;
    @Getter private int expandsDone;

    protected JskCsAbstractImpl(
            List<SOURCE> sources,
            Comparator<ITEM> comparator,
            JskCsInitStrategy<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE> firstFeedStrategy,
            JskCsExpandElementsStrategy<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE> getMoreStrategy) {

        this.sources = new JskCsSources<>(sources);
        this.comparator = comparator;
        this.firstFeedStrategy = firstFeedStrategy;
        this.getMoreStrategy = getMoreStrategy;
    }

    @Override
    public List<ITEM> getNext(int count, EXPAND_DIRECTION direction) {
        initIfNeeded(count);

        List<ITEM> toRet = new ArrayList<>();
        traverseQueue(toRet, count, () -> queue.poll(direction));
        return toRet;
    }

    @Override
    public boolean hasNext(int initializingCount, EXPAND_DIRECTION direction) {
        initIfNeeded(initializingCount);

        return queue.getDirectionIterators().get(direction).hasNext();
    }

    protected abstract QUEUE instantiateQueue();

    protected void traverseQueue(List<ITEM> toRet, int overallCount,
            F0<JskCsPollResult<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> queuePoler) {
        while (toRet.size() < overallCount) {
            final JskCsPollResult<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE> pollResult = queuePoler.apply();
            if (pollResult.getPolledItem().isEmpty()) {
                break;
            }

            final JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE> nextItem = pollResult.getPolledItem().get();
            toRet.add(nextItem.getItem());
            if (nextItem.isExpandable()) {
                onNextExpandableItem(nextItem,
                        overallCount - toRet.size() + 1/*+1 because we have to expand the one we have added*/);
            }
        }
    }

    protected void onNextExpandableItem(JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE> nextItem, int itemsLeft) {
        final Map<SRC_ID, Map<EXPAND_DIRECTION, JskCsList<ITEM>>> afterNextItemExpansion =
                getMoreStrategy.onNextLastItem(nextItem, queue.getDirectionIterators(), itemsLeft);
        O<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> expandedItem = clearPreviousLastItem(nextItem.getSource().getId());
        processSourceRequestResult(afterNextItemExpansion, expandedItem);
        expandsDone++;
    }

    protected void initIfNeeded(int requestedItemCount) {
        if (!initDone) {
            processSourceRequestResult(firstFeedStrategy.initialize(requestedItemCount, sources), O.empty());
            initDone = true;
        }
    }

    protected void processSourceRequestResult(
            Map<SRC_ID, Map<EXPAND_DIRECTION, JskCsList<ITEM>>> data,
            O<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> expandedItem
    ) {
        List<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> queueAdd = Cc.l();
        data.forEach((genId, list) -> {
            queueAdd.addAll(formJskCsItemsFromList(genId, list, expandedItem));
        });
        queue.addAll(queueAdd);
    }

    protected List<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> formJskCsItemsFromList(
            SRC_ID genId,
            Map<EXPAND_DIRECTION, JskCsList<ITEM>> map,
            O<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> lastExpandedItem) {
        clearPreviousLastItem(genId);
        List<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> toAdd = Cc.l();

        map.forEach(((expandDirection, list) -> {
            final int size = list.getItems().size();
            for (int i = 0; i < size; i++) {
                final boolean isExpandableLastItem = list.isHasMoreElements() && i == size - 1;
                final SOURCE source = sources.getSourcesById().get(genId);
                final JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE> item =
                        new JskCsItem<>(comparator, source, list.getItems().get(i), isExpandableLastItem, expandDirection);
                toAdd.add(item);
                if (isExpandableLastItem) {
                    expandableLastItems.put(source.getId(), item);
                }
            }
        }));

        return toAdd;
    }

    protected O<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> clearPreviousLastItem(SRC_ID genId) {
        return O.ofNull(expandableLastItems.get(genId)).map($ -> {
            //clear previous last item
            $.setExpandable(false);
            expandableLastItems.remove(genId);
            return $;
        });
    }
}
