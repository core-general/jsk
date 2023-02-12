package sk.utils.collections.cluster_sorter.forward.impl;

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

import sk.utils.collections.cluster_sorter.abstr.*;
import sk.utils.collections.cluster_sorter.forward.JskCsForward;
import sk.utils.collections.cluster_sorter.forward.impl.strategies.JskCsForwardExpandBatchGreedyImpl;
import sk.utils.collections.cluster_sorter.forward.impl.strategies.JskCsForwardExpandElementSimpleImpl;
import sk.utils.collections.cluster_sorter.forward.impl.strategies.JskCsForwardInitSimpleImpl;
import sk.utils.collections.cluster_sorter.forward.model.JskCsForwardType;

import java.util.Comparator;
import java.util.List;

/**
 * !!!NOT THREAD SAFE!!!
 *
 * @param <SRC_ID>
 * @param <ITEM>
 */
public class JskCsForwardImpl<SRC_ID, ITEM, SOURCE extends JskCsSource<SRC_ID, ITEM>>
        extends
        JskCsAbstractImpl<SRC_ID, ITEM, JskCsForwardType, JskCsQueueForwardImpl<SRC_ID, ITEM, SOURCE>, SOURCE>
        implements JskCsForward<SRC_ID, ITEM> {

    public static <SRC_ID, ITEM, SOURCE extends JskCsSource<SRC_ID, ITEM>> JskCsForwardImpl<SRC_ID, ITEM, SOURCE> custom(
            List<SOURCE> sources,
            Comparator<ITEM> comparator,
            JskCsInitStrategy<SRC_ID, ITEM, JskCsForwardType, SOURCE> firstFeedStrategy,
            JskCsExpandElementsStrategy<SRC_ID, ITEM, JskCsForwardType, SOURCE> getMoreStrategy) {
        return new JskCsForwardImpl<>(sources, comparator, firstFeedStrategy, getMoreStrategy);
    }

    public static <SRC_ID, ITEM, SOURCE extends JskCsSource<SRC_ID, ITEM>> JskCsForwardImpl<SRC_ID, ITEM, SOURCE> simple(
            List<SOURCE> sources,
            Comparator<ITEM> comparator) {
        return new JskCsForwardImpl<>(sources, comparator, new JskCsForwardInitSimpleImpl<>(),
                new JskCsForwardExpandElementSimpleImpl<>());
    }

    public static <SRC_ID, ITEM, SOURCE extends JskCsSource<SRC_ID, ITEM>> JskCsForwardImpl<SRC_ID, ITEM, SOURCE> batch(
            List<SOURCE> sources,
            Comparator<ITEM> comparator, JskCsBatchProcessor<SRC_ID, ITEM, JskCsForwardType> batchProcessor) {
        return new JskCsForwardImpl<>(sources, comparator, new JskCsForwardInitSimpleImpl<>(),
                new JskCsForwardExpandBatchGreedyImpl<>(batchProcessor));
    }

    private JskCsForwardImpl(
            List<SOURCE> sources,
            Comparator<ITEM> comparator,
            JskCsInitStrategy<SRC_ID, ITEM, JskCsForwardType, SOURCE> firstFeedStrategy,
            JskCsExpandElementsStrategy<SRC_ID, ITEM, JskCsForwardType, SOURCE> getMoreStrategy) {
        super(sources, comparator, firstFeedStrategy, getMoreStrategy);
    }

    @Override
    protected JskCsQueueForwardImpl<SRC_ID, ITEM, SOURCE> instantiateQueue() {
        return new JskCsQueueForwardImpl<>();
    }
}
