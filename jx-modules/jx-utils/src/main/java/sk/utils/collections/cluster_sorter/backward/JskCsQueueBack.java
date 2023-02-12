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

import sk.utils.collections.cluster_sorter.abstr.JskCsQueueAbstract;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsPollResult;
import sk.utils.collections.cluster_sorter.backward.model.JskCsBothType;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.Iterator;

public interface JskCsQueueBack<SRC_ID, ITEM, SOURCE extends JskCsSourceBack<SRC_ID, ITEM, JskCsBothType>>
        extends JskCsQueueAbstract<SRC_ID, ITEM, JskCsBothType, SOURCE> {

    public O<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> setLastSelectedItemAndReturnLastUsed(ITEM newItem);

    default JskCsPollResult<SRC_ID, ITEM, JskCsBothType, SOURCE> poll() {
        return poll(JskCsBothType.FORWARD);
    }

    default Iterator<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> iterator() {
        return getDirectionIterators().get(JskCsBothType.FORWARD);
    }

    default JskCsPollResult<SRC_ID, ITEM, JskCsBothType, SOURCE> pollBack() {
        return poll(JskCsBothType.BACKWARD);
    }

    default Iterator<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> iteratorBack() {
        return getDirectionIterators().get(JskCsBothType.BACKWARD);
    }

    default public int calculateSize() {
        return calculateSize(Cc.s(JskCsBothType.FORWARD));
    }

    default public int calculateSizeBack() {
        return calculateSize(Cc.s(JskCsBothType.BACKWARD));
    }
}
