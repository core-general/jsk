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
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.*;

/**
 * Queue based on ArrayList. The last element is the head of the queue.
 * When removing last element, there is no array copy.
 * Items are stored in the order of item comparator.
 */
public class JskCsQueueForwardImpl<ITEM, SOURCE extends JskCsSource<ITEM>>
        extends
        JskCsQueueAbstractImpl<ITEM, JskCsForwardType, SOURCE>
        implements
        JskCsQueueForward<ITEM, SOURCE> {
    private List<JskCsItem<ITEM, JskCsForwardType, SOURCE>> forwardItems = new ArrayList<>();
    private O<JskCsItem<ITEM, JskCsForwardType, SOURCE>> lastConsumed = O.empty();


    @Override
    public O<JskCsItem<ITEM, JskCsForwardType, SOURCE>> getLastConsumedItem() {
        return lastConsumed;
    }

    @Override
    public JskCsPollResult<ITEM, JskCsForwardType, SOURCE> poll(JskCsForwardType jskCsForwardType) {
        JskCsPollResult<ITEM, JskCsForwardType, SOURCE> item = uniPoll(forwardItems, JskCsForwardType.FORWARD);
        item.getPolledItem().ifPresent(it -> lastConsumed = O.of(it));
        return item;
    }

    @Override
    public Map<JskCsForwardType, Iterator<JskCsItem<ITEM, JskCsForwardType, SOURCE>>> getDirectionIterators() {
        return Cc.m(JskCsForwardType.FORWARD, new JskCsItemIterator<>(forwardItems, () -> modCount));
    }

    public List<JskCsItem<ITEM, JskCsForwardType, SOURCE>> getForwardItems() {
        return Collections.unmodifiableList(forwardItems);
    }

    @Override
    protected List<JskCsItem<ITEM, JskCsForwardType, SOURCE>> getQueuePartToAddElements() {
        return forwardItems;
    }

    @Override
    public void addAllToQueueBeginning(List<JskCsItem<ITEM, JskCsForwardType, SOURCE>> jskCsItems) {
        lastConsumed = O.empty();
        addAllRespectConsumed(jskCsItems);
    }
}
