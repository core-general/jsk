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
import sk.utils.collections.cluster_sorter.abstr.JcsInitStrategy;
import sk.utils.collections.cluster_sorter.abstr.model.JcsList;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSourceId;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSources;
import sk.utils.collections.cluster_sorter.backward.model.JcsEBackType;
import sk.utils.collections.cluster_sorter.backward.model.JcsIBackSource;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;

import java.util.Map;

@AllArgsConstructor
public class JcsBackInitStrategyBatch
        <ITEM, SOURCE extends JcsIBackSource<ITEM>>
        implements JcsInitStrategy<ITEM, JcsEBackType, SOURCE> {

    private final JcsIBackBatch<ITEM, SOURCE> batchProcessor;

    @Override
    public Map<JcsSourceId, Map<JcsEBackType, JcsList<ITEM>>>
    initialize(int requestedItemCount, JcsSources<ITEM, SOURCE> sources, boolean isStartingPosition) {
        final int numToSelectPerSource =
                (requestedItemCount / sources.getSourcesById().size()) + 1;

        Map<JcsEBackType, Integer> selector = Cc.m(JcsEBackType.FORWARD, numToSelectPerSource);
        if (!isStartingPosition) {
            selector.put(JcsEBackType.BACKWARD, numToSelectPerSource);
        }
        Map<JcsSourceId, Map<JcsEBackType, Integer>> toBatch = sources.getSourcesById().entrySet().stream()
                .map($ -> X.x($.getKey(), selector))
                .collect(Cc.toMX2());

        return batchProcessor.getNextElements(sources.getSourcesById().values(), toBatch);
    }
}
