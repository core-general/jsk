package sk.utils.collections.cluster_sorter.abstr;

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

import sk.utils.collections.cluster_sorter.abstr.model.JskCsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsPollResult;
import sk.utils.functional.F0;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

public abstract class JskCsQueueAbstractImpl<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE extends JskCsSource<SRC_ID, ITEM>>
        implements JskCsQueueAbstract<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE> {
    protected long modCount = 0;

    protected JskCsPollResult<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE> uniPoll(
            List<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> uniItems, EXPAND_DIRECTION direction) {
        modCount++;
        return new JskCsPollResult<>(uniItems.size() > 0 ? O.of(uniItems.remove(uniItems.size() - 1)) : O.empty(), direction);
    }

    protected void uniAddAll(List<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> data,
            List<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> target,
            F1<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>, Comparator<ITEM>> comparatorSupplier) {
        if (data.size() > 0) {
            modCount++;
            target.addAll(data);
            final Comparator<ITEM> reversed = comparatorSupplier.apply(target.get(0));
            Cc.sort(target, (o1, o2) -> reversed.compare(o1.getItem(), o2.getItem()));
        }
    }

    protected static class JskCsItemIterator<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE extends JskCsSource<SRC_ID, ITEM>>
            implements Iterator<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> {
        private final List<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> items;
        int currentIndex;
        private final F0<Long> modCountProvider;
        final long curModCount;

        public JskCsItemIterator(List<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> items, F0<Long> modCountProvider) {
            this.items = items;
            this.currentIndex = items.size();
            this.modCountProvider = modCountProvider;
            curModCount = modCountProvider.apply();
        }

        @Override
        public boolean hasNext() {
            checkModCount();
            return currentIndex > 0;
        }

        @Override
        public JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE> next() {
            checkModCount();
            return items.get(--currentIndex);
        }

        private void checkModCount() {
            if (modCountProvider.apply() != curModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

}
