package sk.utils.collections.cluster_sorter.forward.impl.strategies;

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

import sk.utils.collections.cluster_sorter.abstr.JcsISource;
import sk.utils.collections.cluster_sorter.abstr.JcsInitStrategy;
import sk.utils.collections.cluster_sorter.abstr.model.JcsList;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSourceId;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSources;
import sk.utils.collections.cluster_sorter.forward.model.JcsEForwardType;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;

import java.util.Map;

public class JcsForwardInitStrategySimple
        <ITEM, SOURCE extends JcsISource<ITEM>>
        implements JcsInitStrategy<ITEM, JcsEForwardType, SOURCE> {
    @Override
    public Map<JcsSourceId, Map<JcsEForwardType, JcsList<ITEM>>>
    initialize(int requestedItemCount, JcsSources<ITEM, SOURCE> sources, boolean isStartingPosition) {
        final int numToSelectPerSource = (requestedItemCount / sources.getSourcesById().size()) + 1;
        return sources.getSourcesById().values()
                .stream()
                .map(source -> X.x(source.getSourceId(),
                        Cc.m(JcsEForwardType.FORWARD, source.getNextUnseenElements(numToSelectPerSource))))
                .collect(Cc.toMX2());
    }
}
