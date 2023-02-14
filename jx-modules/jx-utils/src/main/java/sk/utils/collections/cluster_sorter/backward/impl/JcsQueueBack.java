package sk.utils.collections.cluster_sorter.backward.impl;

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
import sk.utils.collections.cluster_sorter.backward.JcsIQueueBack;
import sk.utils.collections.cluster_sorter.backward.model.JcsEBackType;
import sk.utils.functional.O;
import sk.utils.functional.P1;
import sk.utils.statics.Cc;

import java.util.*;

public class JcsQueueBack<ITEM, SOURCE extends JcsISource<ITEM>>
        extends JcsAQueue<ITEM, JcsEBackType, SOURCE>
        implements JcsIQueueBack<ITEM, SOURCE> {
    protected List<JcsItem<ITEM, JcsEBackType, SOURCE>> forwardItems = new ArrayList<>();
    protected List<JcsItem<ITEM, JcsEBackType, SOURCE>> backItems = new ArrayList<>();

    public List<JcsItem<ITEM, JcsEBackType, SOURCE>> getForwardItems() {
        return Collections.unmodifiableList(forwardItems);
    }

    public List<JcsItem<ITEM, JcsEBackType, SOURCE>> getBackItems() {
        return Collections.unmodifiableList(backItems);
    }

    @Override
    public O<JcsItem<ITEM, JcsEBackType, SOURCE>> getLastConsumedItem() {
        return Cc.last(backItems);
    }

    @Override
    public JcsPollResult<ITEM, JcsEBackType, SOURCE> poll(JcsEBackType jskCsBothType) {
        var pollResult = switch (jskCsBothType) {
            case FORWARD -> uniPoll(forwardItems, jskCsBothType);
            case BACKWARD -> uniPoll(backItems, jskCsBothType);
        };
        pollResult.getPolledItem().ifPresent(item -> {
            switch (pollResult.getDirection()) {
                case FORWARD -> backItems.add(item);
                case BACKWARD -> forwardItems.add(item);
            }
        });
        return pollResult;
    }

    @Override
    public Map<JcsEBackType, Iterator<JcsItem<ITEM, JcsEBackType, SOURCE>>> getDirectionIterators() {
        return Cc.m(
                JcsEBackType.FORWARD, new JskCsItemIterator<>(forwardItems, () -> modCount),
                JcsEBackType.BACKWARD, new JskCsItemIterator<>(backItems, () -> modCount)
        );
    }

    /**
     * Is not a part of the interface because could cause ambiguity with actual CS (CS needs not only to poll, but to expand,
     * which queue doesn't know about, probably could be fixed by also adding the callback processor to this method)
     */
    public O<JcsItem<ITEM, JcsEBackType, SOURCE>> setLastSelectedItemAndReturnLastUsed(ITEM newItem) {
        int indexInItems = Cc.firstIndex(forwardItems, item -> item.getComparator().compare(item.getItem(), newItem) <= 0);
        if (indexInItems > -1) {
            int initialSize = forwardItems.size();
            for (int i = initialSize - 1; i >= indexInItems; i--) {
                poll();
            }
        } else {
            int indexInBackItems = Cc.firstIndex(backItems, item -> item.getComparator().compare(item.getItem(), newItem) >= 0);
            if (indexInBackItems > -1) {
                int initialSize = backItems.size();
                for (int i = indexInBackItems; i < initialSize; i++) {
                    pollBack();
                }
            }
        }
        return Cc.last(backItems);
    }

    @Override
    public int calculateSizeBack() {
        return backItems.size();
    }

    protected JcsPollResult<ITEM, JcsEBackType, SOURCE> uniBackPoll(
            List<JcsItem<ITEM, JcsEBackType, SOURCE>> source,
            List<JcsItem<ITEM, JcsEBackType, SOURCE>> target) {
        var polled = uniPoll(source, JcsEBackType.BACKWARD);
        polled.getPolledItem().ifPresent($ -> target.add($));
        return polled;
    }

    @Override
    public void onDidNotGetToMainQueueWhenAddRespectOrder(List<JcsItem<ITEM, JcsEBackType, SOURCE>> items) {
        uniAddAll(items, backItems, item -> item.getComparator());
    }

    @Override
    public List<JcsItem<ITEM, JcsEBackType, SOURCE>> removeElementsIf(P1<JcsItem<ITEM, JcsEBackType, SOURCE>> predicate) {
        List<JcsItem<ITEM, JcsEBackType, SOURCE>> forw = Cc.removeIf(forwardItems, predicate);
        List<JcsItem<ITEM, JcsEBackType, SOURCE>> back = Cc.removeIf(backItems, predicate);
        return Cc.addAll(Cc.l(), forw, back);
    }

    @Override
    public void clear() {
        forwardItems.clear();
        backItems.clear();
    }

    @Override
    protected List<JcsItem<ITEM, JcsEBackType, SOURCE>> getQueuePartToAddElements() {
        return forwardItems;
    }
}
