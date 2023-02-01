package sk.utils.collections.cluster_sorter.impl;

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

import sk.utils.collections.cluster_sorter.JskCsQueue;
import sk.utils.collections.cluster_sorter.model.JskCsItem;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.*;

/**
 * Queue based on ArrayList. The last element is the head of the queue.
 * When removing last element, there is no array copy.
 * Items are stored in the order of item comparator.
 */
public class JskCsQueueImpl<SRC_ID, ITEM> implements JskCsQueue<SRC_ID, ITEM> {
    List<JskCsItem<SRC_ID, ITEM>> items = new ArrayList<>();
    long modCount = 0;

    @Override
    public void addAll(List<JskCsItem<SRC_ID, ITEM>> data) {
        modCount++;
        items.addAll(data);
        if (items.size() > 0) {
            //reversing comparator so that head of the queue is in the end of list
            final Comparator<ITEM> reversed = items.get(0).getComparator().reversed();
            Cc.sort(items, (o1, o2) -> reversed.compare(o1.getItem(), o2.getItem()));
        }
    }

    @Override
    public O<JskCsItem<SRC_ID, ITEM>> poll() {
        modCount++;
        return items.size() > 0 ? O.of(items.remove(items.size() - 1)) : O.empty();
    }

    @Override
    public Iterator<JskCsItem<SRC_ID, ITEM>> iterator() {
        return new Iterator<>() {
            int currentIndex = items.size();
            final long curModCount = modCount;

            @Override
            public boolean hasNext() {
                checkModCount();
                return currentIndex > 0;
            }

            @Override
            public JskCsItem<SRC_ID, ITEM> next() {
                checkModCount();
                return items.get(--currentIndex);
            }

            private void checkModCount() {
                if (modCount != curModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        };
    }
}
