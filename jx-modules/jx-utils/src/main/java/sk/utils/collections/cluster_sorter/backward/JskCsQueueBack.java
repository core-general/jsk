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
import sk.utils.collections.cluster_sorter.abstr.JskCsSource;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsPollResult;
import sk.utils.collections.cluster_sorter.backward.model.JskCsBackType;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.Iterator;

public interface JskCsQueueBack<ITEM, SOURCE extends JskCsSource<ITEM>>
        extends JskCsQueueAbstract<ITEM, JskCsBackType, SOURCE> {

    public O<JskCsItem<ITEM, JskCsBackType, SOURCE>> setLastSelectedItemAndReturnLastUsed(ITEM newItem);

    default JskCsPollResult<ITEM, JskCsBackType, SOURCE> poll() {
        return poll(JskCsBackType.FORWARD);
    }

    default Iterator<JskCsItem<ITEM, JskCsBackType, SOURCE>> iterator() {
        return getDirectionIterators().get(JskCsBackType.FORWARD);
    }

    default JskCsPollResult<ITEM, JskCsBackType, SOURCE> pollBack() {
        return poll(JskCsBackType.BACKWARD);
    }

    default Iterator<JskCsItem<ITEM, JskCsBackType, SOURCE>> iteratorBack() {
        return getDirectionIterators().get(JskCsBackType.BACKWARD);
    }

    default public int calculateSize() {
        return calculateSize(Cc.s(JskCsBackType.FORWARD));
    }

    default public int calculateSizeBack() {
        return calculateSize(Cc.s(JskCsBackType.BACKWARD));
    }
}
