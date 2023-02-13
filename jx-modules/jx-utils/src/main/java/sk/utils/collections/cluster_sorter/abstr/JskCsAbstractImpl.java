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
import sk.utils.collections.cluster_sorter.abstr.model.*;
import sk.utils.functional.F0;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * !!!NOT THREAD SAFE!!!
 */
public abstract class JskCsAbstractImpl<
        ITEM,
        EXPAND_DIRECTION,
        QUEUE extends JskCsQueueAbstract<ITEM, EXPAND_DIRECTION, SOURCE>,
        SOURCE extends JskCsSource<ITEM>
        >
        implements JskCsAbstract<ITEM, EXPAND_DIRECTION, SOURCE> {

    protected final JskCsSources<ITEM, SOURCE> sources;
    protected final Comparator<ITEM> comparator;
    protected final JskCsInitStrategy<ITEM, EXPAND_DIRECTION, SOURCE> firstFeedStrategy;
    protected final JskCsExpandElementsStrategy<ITEM, EXPAND_DIRECTION, SOURCE> getMoreStrategy;

    @Getter protected final QUEUE queue = instantiateQueue();

    protected final Map<JskCsSrcId, JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> expandableLastItems = Cc.m();
    protected boolean initDone;
    @Getter private int expandsDone;

    protected JskCsAbstractImpl(
            List<SOURCE> sources,
            Comparator<ITEM> comparator,
            JskCsInitStrategy<ITEM, EXPAND_DIRECTION, SOURCE> firstFeedStrategy,
            JskCsExpandElementsStrategy<ITEM, EXPAND_DIRECTION, SOURCE> getMoreStrategy) {

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

    @Override
    public Map<JskCsSrcId, SOURCE> getAllSources() {
        return sources.getSourcesById();
    }

    @Override
    public void addNewSource(SOURCE source) {
        //TODO !!!!!!!!!!!!!!!!!!!...
    }

    @Override
    public void removeSource(JskCsSrcId id) {
        //TODO !!!!!!!!!!!!!!!!!!!...
        //boolean removed = sources.removeSource(id);
        //if (removed) {
        //    //todo cache source items
        //
        //    List<JskCsItem< ITEM, EXPAND_DIRECTION, SOURCE>> removedItems =
        //            queue.removeElementsIf(element -> Fu.equal(element.getSource().getId(), id));
        //}
    }

    protected void traverseQueue(List<ITEM> toRet, int overallCount,
            F0<JskCsPollResult<ITEM, EXPAND_DIRECTION, SOURCE>> queuePoler) {
        while (toRet.size() < overallCount) {
            final JskCsPollResult<ITEM, EXPAND_DIRECTION, SOURCE> pollResult = queuePoler.apply();
            if (pollResult.getPolledItem().isEmpty()) {
                break;
            }

            final JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE> nextItem = pollResult.getPolledItem().get();
            toRet.add(nextItem.getItem());
            if (nextItem.isExpandable()) {
                onNextExpandableItem(nextItem,
                        overallCount - toRet.size() + 1/*+1 because we have to expand the one we have added*/);
            }
        }
    }

    protected void onNextExpandableItem(JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE> nextItem, int itemsLeft) {
        final Map<JskCsSrcId, Map<EXPAND_DIRECTION, JskCsList<ITEM>>> afterNextItemExpansion =
                getMoreStrategy.onNextLastItem(nextItem, queue.getDirectionIterators(), itemsLeft);
        O<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> expandedItem = clearPreviousLastItem(nextItem.getSource().getId());
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
            Map<JskCsSrcId, Map<EXPAND_DIRECTION, JskCsList<ITEM>>> data,
            O<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> expandedItem
    ) {
        List<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> queueAdd = Cc.l();
        data.forEach((genId, list) -> {
            queueAdd.addAll(formJskCsItemsFromList(genId, list, expandedItem));
        });
        queue.addAllRespectConsumed(queueAdd);
    }

    protected List<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> formJskCsItemsFromList(
            JskCsSrcId genId,
            Map<EXPAND_DIRECTION, JskCsList<ITEM>> map,
            O<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> lastExpandedItem) {
        clearPreviousLastItem(genId);
        List<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> toAdd = Cc.l();

        map.forEach(((expandDirection, list) -> {
            final int size = list.getItems().size();
            for (int i = 0; i < size; i++) {
                final boolean isExpandableLastItem = list.isHasMoreElements() && i == size - 1;
                final SOURCE source = sources.getSourcesById().get(genId);
                final JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE> item =
                        new JskCsItem<>(comparator, source, list.getItems().get(i), isExpandableLastItem, expandDirection);
                toAdd.add(item);
                if (isExpandableLastItem) {
                    expandableLastItems.put(source.getId(), item);
                }
            }
        }));

        return toAdd;
    }

    protected O<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> clearPreviousLastItem(JskCsSrcId genId) {
        return O.ofNull(expandableLastItems.get(genId)).map($ -> {
            //clear previous last item
            $.setExpandable(false);
            expandableLastItems.remove(genId);
            return $;
        });
    }
}
