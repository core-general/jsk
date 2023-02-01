package sk.utils.collections.cluster_sorter;

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
import sk.utils.collections.cluster_sorter.impl.JskCsGetMoreElementsStrategyBatchGreedyImpl;
import sk.utils.collections.cluster_sorter.impl.JskCsGetMoreElementsStrategySimpleImpl;
import sk.utils.collections.cluster_sorter.impl.JskCsInitializationStrategySimpleImpl;
import sk.utils.collections.cluster_sorter.impl.JskCsQueueImpl;
import sk.utils.collections.cluster_sorter.model.JskCsItem;
import sk.utils.collections.cluster_sorter.model.JskCsList;
import sk.utils.collections.cluster_sorter.model.JskCsSources;
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
public class JskCs<SRC_ID, ITEM> {
    private final JskCsSources<SRC_ID, ITEM> sources;
    private final Comparator<ITEM> comparator;
    private final JskCsInitializationStrategy<SRC_ID, ITEM> firstFeedStrategy;
    private final JskCsGetMoreElementsStrategy<SRC_ID, ITEM> getMoreStrategy;

    private final JskCsQueue<SRC_ID, ITEM> queue = new JskCsQueueImpl<>();
    private final Map<SRC_ID, JskCsItem<SRC_ID, ITEM>> expandableLastItems = Cc.m();
    private boolean initDone;
    @Getter private int expandsDone;

    public static <SRC_ID, ITEM> JskCs<SRC_ID, ITEM> custom(List<? extends JskCsSource<SRC_ID, ITEM>> sources,
            Comparator<ITEM> comparator,
            JskCsInitializationStrategy<SRC_ID, ITEM> firstFeedStrategy,
            JskCsGetMoreElementsStrategy<SRC_ID, ITEM> getMoreStrategy) {
        return new JskCs<>(sources, comparator, firstFeedStrategy, getMoreStrategy);
    }

    public static <SRC_ID, ITEM> JskCs<SRC_ID, ITEM> simple(List<? extends JskCsSource<SRC_ID, ITEM>> sources,
            Comparator<ITEM> comparator) {
        return new JskCs<>(sources, comparator, new JskCsInitializationStrategySimpleImpl<>(),
                new JskCsGetMoreElementsStrategySimpleImpl<>());
    }

    public static <SRC_ID, ITEM> JskCs<SRC_ID, ITEM> batch(List<? extends JskCsSource<SRC_ID, ITEM>> sources,
            Comparator<ITEM> comparator, JscCsBatchProcessor<SRC_ID, ITEM> batchProcessor) {
        return new JskCs<>(sources, comparator, new JskCsInitializationStrategySimpleImpl<>(),
                new JskCsGetMoreElementsStrategyBatchGreedyImpl<>(batchProcessor));
    }

    private JskCs(
            List<? extends JskCsSource<SRC_ID, ITEM>> sources,
            Comparator<ITEM> comparator,
            JskCsInitializationStrategy<SRC_ID, ITEM> firstFeedStrategy,
            JskCsGetMoreElementsStrategy<SRC_ID, ITEM> getMoreStrategy) {

        this.sources = new JskCsSources<>(sources);
        this.comparator = comparator;
        this.firstFeedStrategy = firstFeedStrategy;
        this.getMoreStrategy = getMoreStrategy;
    }

    public List<ITEM> getNext(int count) {
        initIfNeeded(count);

        List<ITEM> toRet = new ArrayList<>();
        traverseQueue(toRet, count);
        return toRet;
    }

    public boolean hasNext(int initializingCount) {
        initIfNeeded(initializingCount);

        return queue.iterator().hasNext();
    }

    JskCsQueue<SRC_ID, ITEM> getQueueForTest() {
        return queue;
    }

    private void traverseQueue(List<ITEM> toRet, int overallCount) {
        while (toRet.size() < overallCount) {
            final O<JskCsItem<SRC_ID, ITEM>> oNextItem = queue.poll();
            if (oNextItem.isEmpty()) {
                break;
            }

            final JskCsItem<SRC_ID, ITEM> nextItem = oNextItem.get();
            toRet.add(nextItem.getItem());
            if (nextItem.isExpandableLastItem()) {
                onNextExpandableItem(nextItem,
                        overallCount - toRet.size() + 1/*+1 because we have to expand the one we have added*/);
            }
        }
    }

    private void onNextExpandableItem(JskCsItem<SRC_ID, ITEM> nextItem, int itemsLeft) {
        final Map<SRC_ID, JskCsList<ITEM>> afterNextItemExpansion =
                getMoreStrategy.onNextLastItem(nextItem, queue.iterator(), itemsLeft);
        clearPreviousLastItem(nextItem.getSource().getId());
        processSourceRequestResult(afterNextItemExpansion);
        expandsDone++;
    }

    private void initIfNeeded(int requestedItemCount) {
        if (!initDone) {
            processSourceRequestResult(firstFeedStrategy.initialize(requestedItemCount, sources));
            initDone = true;
        }
    }

    private void processSourceRequestResult(Map<SRC_ID, JskCsList<ITEM>> initialized) {
        List<JskCsItem<SRC_ID, ITEM>> queueAdd = Cc.l();
        initialized.forEach((genId, list) -> {
            queueAdd.addAll(formJskCsItemsFromList(genId, list));
        });
        queue.addAll(queueAdd);
    }

    private List<JskCsItem<SRC_ID, ITEM>> formJskCsItemsFromList(SRC_ID genId, JskCsList<ITEM> list) {
        clearPreviousLastItem(genId);

        final int size = list.getItems().size();
        List<JskCsItem<SRC_ID, ITEM>> toAdd = Cc.l();
        for (int i = 0; i < size; i++) {
            final boolean isExpandableLastItem = list.isHasMoreElements() && i == size - 1;
            final JskCsSource<SRC_ID, ITEM> source = sources.getSourcesById().get(genId);
            final JskCsItem<SRC_ID, ITEM> item =
                    new JskCsItem<>(comparator, source, list.getItems().get(i), isExpandableLastItem);
            toAdd.add(item);
            if (isExpandableLastItem) {
                expandableLastItems.put(source.getId(), item);
            }
        }
        return toAdd;
    }

    private void clearPreviousLastItem(SRC_ID genId) {
        O.ofNull(expandableLastItems.get(genId)).ifPresent($ -> {
            //clear previous last item
            $.setExpandableLastItem(false);
            expandableLastItems.remove(genId);
        });
    }
}
