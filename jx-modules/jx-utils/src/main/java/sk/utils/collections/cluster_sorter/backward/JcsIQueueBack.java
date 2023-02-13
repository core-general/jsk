package sk.utils.collections.cluster_sorter.backward;

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

import sk.utils.collections.cluster_sorter.abstr.JcsIQueue;
import sk.utils.collections.cluster_sorter.abstr.JcsISource;
import sk.utils.collections.cluster_sorter.abstr.model.JcsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JcsPollResult;
import sk.utils.collections.cluster_sorter.backward.model.JcsEBackType;
import sk.utils.statics.Cc;

import java.util.Iterator;

public interface JcsIQueueBack<ITEM, SOURCE extends JcsISource<ITEM>>
        extends JcsIQueue<ITEM, JcsEBackType, SOURCE> {

    default JcsPollResult<ITEM, JcsEBackType, SOURCE> poll() {
        return poll(JcsEBackType.FORWARD);
    }

    default Iterator<JcsItem<ITEM, JcsEBackType, SOURCE>> iterator() {
        return getDirectionIterators().get(JcsEBackType.FORWARD);
    }

    default JcsPollResult<ITEM, JcsEBackType, SOURCE> pollBack() {
        return poll(JcsEBackType.BACKWARD);
    }

    default Iterator<JcsItem<ITEM, JcsEBackType, SOURCE>> iteratorBack() {
        return getDirectionIterators().get(JcsEBackType.BACKWARD);
    }

    default public int calculateSize() {
        return calculateSize(Cc.s(JcsEBackType.FORWARD));
    }

    default public int calculateSizeBack() {
        return calculateSize(Cc.s(JcsEBackType.BACKWARD));
    }
}
