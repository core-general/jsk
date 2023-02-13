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

import java.util.*;
import java.util.stream.Collectors;

public abstract class JskCsQueueAbstractImpl<ITEM, EXPAND_DIRECTION, SOURCE extends JskCsSource<ITEM>>
        implements JskCsQueueAbstract<ITEM, EXPAND_DIRECTION, SOURCE> {
    protected long modCount = 0;

    protected abstract List<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> getQueuePartToAddElements();

    protected JskCsPollResult<ITEM, EXPAND_DIRECTION, SOURCE> uniPoll(
            List<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> uniItems, EXPAND_DIRECTION direction) {
        modCount++;
        return new JskCsPollResult<>(uniItems.size() > 0 ? O.of(uniItems.remove(uniItems.size() - 1)) : O.empty(), direction);
    }

    protected void uniAddAll(List<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> data,
            List<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> target,
            F1<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>, Comparator<ITEM>> comparatorSupplier) {
        if (data.size() > 0) {
            modCount++;
            target.addAll(data);
            final Comparator<ITEM> reversed = comparatorSupplier.apply(target.get(0));
            Cc.sort(target, (o1, o2) -> reversed.compare(o1.getItem(), o2.getItem()));
        }
    }

    @Override
    public void addAllRespectConsumed(List<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> items) {
        Map<Boolean, List<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>>> split =
                items.stream().collect(Collectors.groupingBy(
                        item -> getLastConsumedItem().map(lastConsumedItem -> item.compareTo(lastConsumedItem) >= 0).orElse(true)
                ));
        List<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> addToBeginning = split.getOrDefault(true, Cc.lEmpty());
        List<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> didNotGetToMainQueue = split.getOrDefault(false, Cc.lEmpty());
        if (addToBeginning.size() > 0) {
            uniAddAll(addToBeginning, getQueuePartToAddElements(), (item) -> item.getComparator().reversed());
        }
        if (didNotGetToMainQueue.size() > 0) {
            onDidNotGetToMainQueueWhenAddRespectOrder(didNotGetToMainQueue);
        }
    }

    protected static class JskCsItemIterator<ITEM, EXPAND_DIRECTION, SOURCE extends JskCsSource<ITEM>>
            implements Iterator<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> {
        private final List<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> items;
        int currentIndex;
        private final F0<Long> modCountProvider;
        final long curModCount;

        public JskCsItemIterator(List<JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE>> items, F0<Long> modCountProvider) {
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
        public JskCsItem<ITEM, EXPAND_DIRECTION, SOURCE> next() {
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
