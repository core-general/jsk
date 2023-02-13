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

import sk.utils.collections.cluster_sorter.abstr.JcsAQueue;
import sk.utils.collections.cluster_sorter.abstr.JcsISource;
import sk.utils.collections.cluster_sorter.abstr.model.JcsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JcsPollResult;
import sk.utils.collections.cluster_sorter.forward.JcsIQueueForward;
import sk.utils.collections.cluster_sorter.forward.model.JcsEForwardType;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.*;

/**
 * Queue based on ArrayList. The last element is the head of the queue.
 * When removing last element, there is no array copy.
 * Items are stored in the order of item comparator.
 */
public class JcsQueueForward<ITEM, SOURCE extends JcsISource<ITEM>>
        extends
        JcsAQueue<ITEM, JcsEForwardType, SOURCE>
        implements
        JcsIQueueForward<ITEM, SOURCE> {
    private List<JcsItem<ITEM, JcsEForwardType, SOURCE>> forwardItems = new ArrayList<>();
    private O<JcsItem<ITEM, JcsEForwardType, SOURCE>> lastConsumed = O.empty();


    @Override
    public O<JcsItem<ITEM, JcsEForwardType, SOURCE>> getLastConsumedItem() {
        return lastConsumed;
    }

    @Override
    public JcsPollResult<ITEM, JcsEForwardType, SOURCE> poll(JcsEForwardType jskCsForwardType) {
        JcsPollResult<ITEM, JcsEForwardType, SOURCE> item = uniPoll(forwardItems, JcsEForwardType.FORWARD);
        item.getPolledItem().ifPresent(it -> lastConsumed = O.of(it));
        return item;
    }

    @Override
    public Map<JcsEForwardType, Iterator<JcsItem<ITEM, JcsEForwardType, SOURCE>>> getDirectionIterators() {
        return Cc.m(JcsEForwardType.FORWARD, new JskCsItemIterator<>(forwardItems, () -> modCount));
    }

    public List<JcsItem<ITEM, JcsEForwardType, SOURCE>> getForwardItems() {
        return Collections.unmodifiableList(forwardItems);
    }

    @Override
    protected List<JcsItem<ITEM, JcsEForwardType, SOURCE>> getQueuePartToAddElements() {
        return forwardItems;
    }

    @Override
    public void addAllToQueueBeginning(List<JcsItem<ITEM, JcsEForwardType, SOURCE>> jskCsItems) {
        lastConsumed = O.empty();
        addAllRespectConsumed(jskCsItems);
    }
}
