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

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * !!!NOT THREAD SAFE!!!
 */
public abstract class JcsASorter<
        ITEM,
        EXPAND_DIRECTION,
        QUEUE extends JcsIQueue<ITEM, EXPAND_DIRECTION, SOURCE>,
        SOURCE extends JcsISource<ITEM>
        >
        implements JcsISorter<ITEM, EXPAND_DIRECTION, SOURCE> {

    protected final JcsSources<ITEM, SOURCE> sources;
    protected final Comparator<ITEM> comparator;
    protected final JcsInitStrategy<ITEM, EXPAND_DIRECTION, SOURCE> firstFeedStrategy;
    protected final JcsIExpandElementsStrategy<ITEM, EXPAND_DIRECTION, SOURCE> getMoreStrategy;

    @Getter protected final QUEUE queue = instantiateQueue();

    protected final Map<JcsSrcId, JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> expandableLastItems = Cc.m();
    protected boolean initDone;
    @Getter private int expandsDone;
    @Getter private int clearsDone;

    protected JcsASorter(
            List<SOURCE> sources,
            Comparator<ITEM> comparator,
            JcsInitStrategy<ITEM, EXPAND_DIRECTION, SOURCE> firstFeedStrategy,
            JcsIExpandElementsStrategy<ITEM, EXPAND_DIRECTION, SOURCE> getMoreStrategy) {

        this.sources = new JcsSources<>(sources);
        this.comparator = comparator;
        this.firstFeedStrategy = firstFeedStrategy;
        this.getMoreStrategy = getMoreStrategy;
    }

    @Override
    public List<ITEM> getNext(int count, EXPAND_DIRECTION direction) {
        return traverseQueue(count, () -> queue.poll(direction));
    }

    @Override
    public boolean hasNext(int initializingCount, EXPAND_DIRECTION direction) {
        initIfNeeded(initializingCount);

        return queue.getDirectionIterators().get(direction).hasNext();
    }

    protected abstract QUEUE instantiateQueue();

    @Override
    public Map<JcsSrcId, SOURCE> getAllSources() {
        return sources.getSourcesById();
    }

    @Override
    public void addNewSource(SOURCE source) {
        //TODO !!!!!!!!!!!!!!!!!!!...
    }

    @Override
    public void removeSource(JcsSrcId id) {
        //TODO !!!!!!!!!!!!!!!!!!!...
        //boolean removed = sources.removeSource(id);
        //if (removed) {
        //    //todo cache source items
        //
        //    List<JcsItem< ITEM, EXPAND_DIRECTION, SOURCE>> removedItems =
        //            queue.removeElementsIf(element -> Fu.equal(element.getSource().getId(), id));
        //}
    }

    @Override
    public void setPositionToItem(ITEM item) {
        //clear all state, set position in all sources, initialize again
        if (!sources.getSourcesById().values().stream().allMatch($ -> $.canSetPosition())) {
            throw new UnsupportedOperationException();
        }
        clearState();
        sources.getSourcesById().values().stream().forEach($ -> $.setPositionToItem(item));
    }

    protected List<ITEM> traverseQueue(int overallCount,
            F0<JcsPollResult<ITEM, EXPAND_DIRECTION, SOURCE>> queuePoler) {
        initIfNeeded(overallCount);

        List<ITEM> toRet = Cc.l();

        while (toRet.size() < overallCount) {
            final JcsPollResult<ITEM, EXPAND_DIRECTION, SOURCE> pollResult = queuePoler.apply();
            if (pollResult.getPolledItem().isEmpty()) {
                break;
            }

            final JcsItem<ITEM, EXPAND_DIRECTION, SOURCE> nextItem = pollResult.getPolledItem().get();
            toRet.add(nextItem.getItem());
            if (nextItem.isExpandable()) {
                onNextExpandableItem(nextItem,
                        overallCount - toRet.size() + 1/*+1 because we have to expand the one we have added*/);
            }
        }
        return toRet;
    }

    protected void onNextExpandableItem(JcsItem<ITEM, EXPAND_DIRECTION, SOURCE> nextItem, int itemsLeft) {
        final Map<JcsSrcId, Map<EXPAND_DIRECTION, JcsList<ITEM>>> afterNextItemExpansion =
                getMoreStrategy.onNextLastItem(nextItem, queue.getDirectionIterators(), itemsLeft);
        O<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> expandedItem = clearPreviousLastItem(nextItem.getSource().getId());
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
            Map<JcsSrcId, Map<EXPAND_DIRECTION, JcsList<ITEM>>> data,
            O<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> expandedItem
    ) {
        List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> queueAdd = Cc.l();
        data.forEach((genId, list) -> {
            queueAdd.addAll(formJskCsItemsFromList(genId, list, expandedItem));
        });
        queue.addAllRespectConsumed(queueAdd);
    }

    protected List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> formJskCsItemsFromList(
            JcsSrcId genId,
            Map<EXPAND_DIRECTION, JcsList<ITEM>> map,
            O<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> lastExpandedItem) {
        clearPreviousLastItem(genId);
        List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> toAdd = Cc.l();

        map.forEach(((expandDirection, list) -> {
            final int size = list.getItems().size();
            for (int i = 0; i < size; i++) {
                final boolean isExpandableLastItem = list.isHasMoreElements() && i == size - 1;
                final SOURCE source = sources.getSourcesById().get(genId);
                final JcsItem<ITEM, EXPAND_DIRECTION, SOURCE> item =
                        new JcsItem<>(comparator, source, list.getItems().get(i), isExpandableLastItem, expandDirection);
                toAdd.add(item);
                if (isExpandableLastItem) {
                    expandableLastItems.put(source.getId(), item);
                }
            }
        }));

        return toAdd;
    }

    protected O<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> clearPreviousLastItem(JcsSrcId genId) {
        return O.ofNull(expandableLastItems.get(genId)).map($ -> {
            //clear previous last item
            $.setExpandable(false);
            expandableLastItems.remove(genId);
            return $;
        });
    }

    protected void clearState() {
        queue.clear();
        expandableLastItems.clear();
        initDone = false;
        clearsDone++;
    }
}
