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

import sk.utils.collections.cluster_sorter.abstr.JcsInitStrategy;
import sk.utils.collections.cluster_sorter.abstr.model.JcsList;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSources;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSrcId;
import sk.utils.collections.cluster_sorter.backward.model.JcsEBackType;
import sk.utils.collections.cluster_sorter.backward.model.JcsIBackSource;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;

import java.util.Map;

public class JcsBackInitStrategySimple
        <ITEM, SOURCE extends JcsIBackSource<ITEM>>
        implements JcsInitStrategy<ITEM, JcsEBackType, SOURCE> {
    @Override
    public Map<JcsSrcId, Map<JcsEBackType, JcsList<ITEM>>>
    initialize(int requestedItemCount, JcsSources<ITEM, SOURCE> sources, boolean isStartingPosition) {
        final int numToSelectPerSource = (requestedItemCount / sources.getSourcesById().size()) + 1;
        return sources.getSourcesById().values()
                .stream()
                .map(source -> {
                    Map<JcsEBackType, JcsList<ITEM>> toGet =
                            Cc.m(JcsEBackType.FORWARD, source.getNextElements(numToSelectPerSource));
                    if (!isStartingPosition) {
                        toGet.put(JcsEBackType.BACKWARD, source.getPreviousElements(numToSelectPerSource));
                    }

                    return X.x(source.getId(), toGet);
                })
                .collect(Cc.toMX2());
    }
}
