package sk.utils.collections.cluster_sorter.forward.impl;

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

import sk.utils.collections.cluster_sorter.abstr.JskCsQueueAbstractImpl;
import sk.utils.collections.cluster_sorter.abstr.JskCsSource;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsPollResult;
import sk.utils.collections.cluster_sorter.forward.JskCsQueueForward;
import sk.utils.collections.cluster_sorter.forward.model.JskCsForwardType;
import sk.utils.statics.Cc;

import java.util.*;

/**
 * Queue based on ArrayList. The last element is the head of the queue.
 * When removing last element, there is no array copy.
 * Items are stored in the order of item comparator.
 */
public class JskCsQueueForwardImpl<SRC_ID, ITEM, SOURCE extends JskCsSource<SRC_ID, ITEM>>
        extends
        JskCsQueueAbstractImpl<SRC_ID, ITEM, JskCsForwardType, SOURCE>
        implements
        JskCsQueueForward<SRC_ID, ITEM, SOURCE> {
    protected List<JskCsItem<SRC_ID, ITEM, JskCsForwardType, SOURCE>> forwardItems = new ArrayList<>();

    /** Semantics: we add all elements to the queue, doesn't matter if they are "before" current element */
    @Override
    public void addAll(List<JskCsItem<SRC_ID, ITEM, JskCsForwardType, SOURCE>> newData) {
        uniAddAll(newData, forwardItems, item -> item.getComparator().reversed());
    }

    @Override
    public JskCsPollResult<SRC_ID, ITEM, JskCsForwardType, SOURCE> poll(JskCsForwardType jskCsForwardType) {
        return uniPoll(forwardItems, JskCsForwardType.FORWARD);
    }

    @Override
    public Map<JskCsForwardType, Iterator<JskCsItem<SRC_ID, ITEM, JskCsForwardType, SOURCE>>> getDirectionIterators() {
        return Cc.m(JskCsForwardType.FORWARD, new JskCsItemIterator<>(forwardItems, () -> modCount));
    }

    public List<JskCsItem<SRC_ID, ITEM, JskCsForwardType, SOURCE>> getForwardItems() {
        return Collections.unmodifiableList(forwardItems);
    }
}
