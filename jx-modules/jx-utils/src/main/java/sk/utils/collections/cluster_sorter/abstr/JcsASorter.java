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
import sk.utils.statics.Fu;
import sk.utils.tuples.X;

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
    protected final JcsInitStrategy<ITEM, EXPAND_DIRECTION, SOURCE> initSourceStrategy;
    protected final JcsIExpandElementsStrategy<ITEM, EXPAND_DIRECTION, SOURCE> getMoreFromSourceStrategy;

    @Getter protected final QUEUE queue = instantiateQueue();

    protected final Map<JcsSourceId, Map<EXPAND_DIRECTION, JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>>> expandableLastItems = Cc.m();
    protected boolean initDone;
    protected O<ITEM> positionSet = O.empty();
    @Getter private int expandsDone;
    @Getter private int clearsDone;

    protected JcsASorter(
            List<SOURCE> sources,
            Comparator<ITEM> comparator,
            JcsInitStrategy<ITEM, EXPAND_DIRECTION, SOURCE> initSourceStrategy,
            JcsIExpandElementsStrategy<ITEM, EXPAND_DIRECTION, SOURCE> getMoreFromSourceStrategy) {

        this.sources = new JcsSources<>(sources);
        this.comparator = comparator;
        this.initSourceStrategy = initSourceStrategy;
        this.getMoreFromSourceStrategy = getMoreFromSourceStrategy;
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

    @Override
    public Map<JcsSourceId, SOURCE> getAllSources() {
        return sources.getSourcesById();
    }

    @Override
    public List<ITEM> removeSource(JcsSourceId id) {
        boolean removed = sources.removeSource(id);
        if (removed) {
            List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> removedItems =
                    queue.removeElementsIf(element -> Fu.equal(element.getSource().getSourceId(), id));
            return removedItems.stream().map($ -> $.getItem()).toList();
        } else {
            return Cc.lEmpty();
        }
    }

    @Override
    public final void addNewSource(SOURCE source) {
        if (sources.getSourcesById().containsKey(source.getSourceId())) {
            throw new RuntimeException("Source with id: %s already exist".formatted(source.getSourceId()));
        }
        positionSet.ifPresent($ -> source.setPositionToItem($));
        addNewSourcePrivate(source,
                initSourceStrategy.initialize(5, new JcsSources<>(Cc.l(source)), false).get(source.getSourceId()),
                positionSet);
        sources.addSource(source);
    }

    @Override
    public final void setPositionToItem(ITEM item) {
        //clear all state, set position in all sources, initialize again
        if (!sources.getSourcesById().values().stream().allMatch($ -> $.canSetPosition())) {
            throw new UnsupportedOperationException();
        }
        clearState();
        positionSet = O.of(item);
        sources.getSourcesById().values().stream().forEach($ -> $.setPositionToItem(item));
    }


    protected abstract QUEUE instantiateQueue();

    protected abstract void addNewSourcePrivate(
            SOURCE source, Map<EXPAND_DIRECTION, JcsList<ITEM>> initialSourceItems, O<ITEM> positionSet);


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
        final Map<JcsSourceId, JcsList<ITEM>> afterNextItemExpansion =
                getMoreFromSourceStrategy.getMoreFromSourceInDirection(nextItem.getSource(), nextItem.getExpandDirection(),
                        queue.getDirectionIterators().get(nextItem.getExpandDirection()), itemsLeft);
        clearPreviousLastItem(nextItem.getSource().getSourceId(), nextItem.getExpandDirection());
        processSourceRequestResult(
                afterNextItemExpansion.entrySet().stream()
                        .map($ -> X.x($.getKey(), Cc.m(nextItem.getExpandDirection(), $.getValue())))
                        .collect(Cc.toMX2()), sources, O.empty());
        expandsDone++;
    }

    protected void initIfNeeded(int requestedItemCount) {
        if (!initDone) {
            Map<JcsSourceId, Map<EXPAND_DIRECTION, JcsList<ITEM>>> initialize =
                    initSourceStrategy.initialize(requestedItemCount, sources, positionSet.isEmpty());
            initialize.forEach((k, v) -> {
                addNewSourcePrivate(sources.getById(k), v, positionSet);
            });
            initDone = true;
        }
    }

    protected void processSourceRequestResult(
            Map<JcsSourceId, Map<EXPAND_DIRECTION, JcsList<ITEM>>> data,
            JcsSources<ITEM, SOURCE> sources,
            O<ITEM> position
    ) {
        List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> queueAdd = Cc.l();
        data.forEach((genId, list) -> {
            queueAdd.addAll(formJskCsItemsFromList(sources.getById(genId), list));
        });
        if (position.isPresent()) {
            queue.addAllRespectItem(queueAdd, position);
        } else {
            queue.addAllRespectConsumed(queueAdd);
        }

    }

    protected List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> formJskCsItemsFromList(
            SOURCE source,
            Map<EXPAND_DIRECTION, JcsList<ITEM>> map) {
        List<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> toAdd = Cc.l();

        map.forEach(((expandDirection, list) -> {
            clearPreviousLastItem(source.getSourceId(), expandDirection);
            final int size = list.getItems().size();
            for (int i = 0; i < size; i++) {
                final boolean isExpandableLastItem = list.isHasMoreElements() && i == size - 1;
                final JcsItem<ITEM, EXPAND_DIRECTION, SOURCE> item =
                        new JcsItem<>(comparator, source, list.getItems().get(i), isExpandableLastItem, expandDirection);
                toAdd.add(item);
                if (isExpandableLastItem) {
                    Cc.computeAndApply(expandableLastItems, source.getSourceId(),
                            (id, mp) -> Cc.put(mp, expandDirection, item), () -> Cc.m());
                }
            }
        }));

        return toAdd;
    }

    protected O<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> clearPreviousLastItem(JcsSourceId genId, EXPAND_DIRECTION direction) {
        return O.ofNull(expandableLastItems.get(genId))
                .flatMap($ -> O.ofNull($.get(direction)))
                .map($ -> {
                    //clear previous last item
                    $.setExpandable(false);
                    var realMap = expandableLastItems.getOrDefault(genId, Cc.m());
                    realMap.remove(direction);
                    if (realMap.size() == 0) {
                        expandableLastItems.remove(genId);
                    }
                    return $;
                });
    }

    protected void clearState() {
        queue.clear();
        expandableLastItems.clear();
        initDone = false;
        positionSet = O.empty();
        clearsDone++;
    }
}
