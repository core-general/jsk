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

import sk.utils.collections.cluster_sorter.abstr.JcsIExpandElementsStrategy;
import sk.utils.collections.cluster_sorter.abstr.model.JcsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JcsList;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSrcId;
import sk.utils.collections.cluster_sorter.backward.model.JcsEBackType;
import sk.utils.collections.cluster_sorter.backward.model.JcsIBackSource;
import sk.utils.statics.Cc;

import java.util.Iterator;
import java.util.Map;

public class JcsBackExpandStrategySimple<ITEM, SOURCE extends JcsIBackSource<ITEM>>
        implements JcsIExpandElementsStrategy<ITEM, JcsEBackType, SOURCE> {

    @Override
    public Map<JcsSrcId, JcsList<ITEM>>
    getMoreFromSourceInDirection(SOURCE source,
            JcsEBackType direction,
            Iterator<JcsItem<ITEM, JcsEBackType, SOURCE>> sortedRestOfQueuePath,
            int itemsLeft) {
        return Cc.m(source.getId(), switch (direction) {
            case FORWARD -> source.getNextElements(itemsLeft);
            case BACKWARD -> source.getPreviousElements(itemsLeft);
        });
    }
}
