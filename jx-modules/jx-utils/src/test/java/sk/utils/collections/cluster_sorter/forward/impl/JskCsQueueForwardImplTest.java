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

import org.junit.Test;
import sk.utils.collections.cluster_sorter.JskCsAbstractQueueTest;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsItem;
import sk.utils.collections.cluster_sorter.forward.model.JskCsForwardType;
import sk.utils.statics.Cc;

import static org.junit.Assert.assertEquals;
import static sk.utils.collections.cluster_sorter.forward.model.JskCsForwardType.FORWARD;

public class JskCsQueueForwardImplTest extends JskCsAbstractQueueTest<JskCsForwardType, JskCsQueueForwardImpl<?, Integer, ?>> {

    protected JskCsQueueForwardImpl<?, Integer, ?> initQueue() {
        return new JskCsQueueForwardImpl<>();
    }

    @Override
    protected JskCsForwardType getForwardDirection() {
        return FORWARD;
    }


    @Test
    public void addAll() {
        assertEquals("1,1,2,3,5,7",
                Cc.join(Cc.list(queue.getDirectionIterators().get(getForwardDirection())).stream().map($ -> $.getItem() + "")));
        assertEquals("7,5,3,2,1,1", Cc.join(queue.getForwardItems()));
    }


    @Test
    public void poll() {
        assertEquals(1, queue.poll().getPolledItem().get().getItem().longValue());
        assertEquals(1, queue.poll().getPolledItem().get().getItem().longValue());
        assertEquals(2, queue.poll().getPolledItem().get().getItem().longValue());

        queue.addAll(Cc.l(
                new JskCsItem<>(Integer::compareTo, null, 1, false, null),
                new JskCsItem<>(Integer::compareTo, null, 8, false, null),
                new JskCsItem<>(Integer::compareTo, null, 2, false, null)
        ));

        assertEquals(1, queue.poll().getPolledItem().get().getItem().longValue());
        assertEquals(2, queue.poll().getPolledItem().get().getItem().longValue());
        assertEquals(3, queue.poll().getPolledItem().get().getItem().longValue());
        assertEquals(5, queue.poll().getPolledItem().get().getItem().longValue());
        assertEquals(7, queue.poll().getPolledItem().get().getItem().longValue());
        assertEquals(8, queue.poll().getPolledItem().get().getItem().longValue());
    }
}
