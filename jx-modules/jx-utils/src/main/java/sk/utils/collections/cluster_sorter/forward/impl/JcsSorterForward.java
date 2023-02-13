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
import sk.utils.collections.cluster_sorter.forward.JcsISorterForward;
import sk.utils.collections.cluster_sorter.forward.impl.strategies.JcsForwardExpandStrategyBatch;
import sk.utils.collections.cluster_sorter.forward.impl.strategies.JcsForwardExpandStrategySimple;
import sk.utils.collections.cluster_sorter.forward.impl.strategies.JcsForwardInitStrategyBatch;
import sk.utils.collections.cluster_sorter.forward.impl.strategies.JcsForwardInitStrategySimple;
import sk.utils.collections.cluster_sorter.forward.model.JcsEForwardType;

import java.util.Comparator;
import java.util.List;

/**
 * !!!NOT THREAD SAFE!!!
 */
public class JcsSorterForward<ITEM, SOURCE extends JcsISource<ITEM>>
        extends
        JcsASorter<ITEM, JcsEForwardType, JcsQueueForward<ITEM, SOURCE>, SOURCE>
        implements JcsISorterForward<ITEM, SOURCE> {

    public static <ITEM, SOURCE extends JcsISource<ITEM>> JcsSorterForward<ITEM, SOURCE> custom(
            List<SOURCE> sources,
            Comparator<ITEM> comparator,
            JcsInitStrategy<ITEM, JcsEForwardType, SOURCE> firstFeedStrategy,
            JcsIExpandElementsStrategy<ITEM, JcsEForwardType, SOURCE> getMoreStrategy) {
        return new JcsSorterForward<>(sources, comparator, firstFeedStrategy, getMoreStrategy);
    }

    public static <ITEM, SOURCE extends JcsISource<ITEM>> JcsSorterForward<ITEM, SOURCE> simple(
            List<SOURCE> sources,
            Comparator<ITEM> comparator) {
        return new JcsSorterForward<>(sources, comparator, new JcsForwardInitStrategySimple<>(),
                new JcsForwardExpandStrategySimple<>());
    }

    public static <ITEM, SOURCE extends JcsISource<ITEM>> JcsSorterForward<ITEM, SOURCE> batch(
            List<SOURCE> sources,
            Comparator<ITEM> comparator, JcsIBatchProcessor<ITEM, JcsEForwardType, SOURCE> batchProcessor) {
        return new JcsSorterForward<>(sources, comparator, new JcsForwardInitStrategyBatch<>(batchProcessor),
                new JcsForwardExpandStrategyBatch<>(batchProcessor));
    }

    private JcsSorterForward(
            List<SOURCE> sources,
            Comparator<ITEM> comparator,
            JcsInitStrategy<ITEM, JcsEForwardType, SOURCE> firstFeedStrategy,
            JcsIExpandElementsStrategy<ITEM, JcsEForwardType, SOURCE> getMoreStrategy) {
        super(sources, comparator, firstFeedStrategy, getMoreStrategy);
    }

    @Override
    protected JcsQueueForward<ITEM, SOURCE> instantiateQueue() {
        return new JcsQueueForward<>();
    }
}
