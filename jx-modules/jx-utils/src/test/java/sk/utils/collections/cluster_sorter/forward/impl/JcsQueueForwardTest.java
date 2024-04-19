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

import org.junit.jupiter.api.Test;
import sk.utils.collections.cluster_sorter.JcsAbstractQueueTest;
import sk.utils.collections.cluster_sorter.abstr.model.JcsItem;
import sk.utils.collections.cluster_sorter.forward.model.JcsEForwardType;
import sk.utils.statics.Cc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sk.utils.collections.cluster_sorter.forward.model.JcsEForwardType.FORWARD;

public class JcsQueueForwardTest extends JcsAbstractQueueTest<JcsEForwardType, JcsQueueForward<Integer, ?>> {

    protected JcsQueueForward<Integer, ?> initQueue() {
        return new JcsQueueForward<>();
    }

    @Override
    protected JcsEForwardType getForwardDirection() {
        return FORWARD;
    }


    @Test
    public void addAll() {
        assertEquals("1,1,2,3,5,7",
                Cc.join(Cc.list(queue.getDirectionIterators().get(getForwardDirection())).stream().map($ -> $.getItem() + "")));
        assertEquals("7,5,3,2,1,1", Cc.join(queue.getForwardItems()));
    }

    @Test
    public void removeIfTest() {
        assertEquals("7,5,3,2,1,1", Cc.join((queue.getForwardItems())));

        assertEquals("7,5", Cc.join(queue.removeElementsIf(el -> el.getItem() > 3)));
        assertEquals("3,2,1,1", Cc.join((queue.getForwardItems())));

        queue.poll();
        queue.poll();
        queue.poll();
        assertEquals("3", Cc.join((queue.getForwardItems())));

        assertEquals("3", Cc.join(queue.removeElementsIf(el -> el.getItem() > 2)));
        assertEquals("", Cc.join((queue.getForwardItems())));

        //checking that last consume also work
        queue.addAllRespectConsumed(Cc.l(
                new JcsItem<>(Integer::compareTo, null, 1, false, null)
        ), getForwardDirection());
        assertEquals("", Cc.join((queue.getForwardItems())));
    }


    @Test
    public void poll() {
        assertEquals(1, queue.poll().getPolledItem().get().getItem().longValue());
        assertEquals(1, queue.poll().getPolledItem().get().getItem().longValue());
        assertEquals(2, queue.poll().getPolledItem().get().getItem().longValue());

        queue.addAllToQueueBeginning(Cc.l(
                new JcsItem<>(Integer::compareTo, null, 1, false, null),
                new JcsItem<>(Integer::compareTo, null, 8, false, null),
                new JcsItem<>(Integer::compareTo, null, 2, false, null)
        ));

        assertEquals(1, queue.poll().getPolledItem().get().getItem().longValue());
        assertEquals(2, queue.poll().getPolledItem().get().getItem().longValue());
        assertEquals(3, queue.poll().getPolledItem().get().getItem().longValue());
        assertEquals(5, queue.poll().getPolledItem().get().getItem().longValue());
        assertEquals(7, queue.poll().getPolledItem().get().getItem().longValue());
        assertEquals(8, queue.poll().getPolledItem().get().getItem().longValue());

        queue.addAllRespectConsumed(Cc.l(
                new JcsItem<>(Integer::compareTo, null, 4, false, null),
                new JcsItem<>(Integer::compareTo, null, 12, false, null),
                new JcsItem<>(Integer::compareTo, null, 8, false, null),
                new JcsItem<>(Integer::compareTo, null, 15, false, null),
                new JcsItem<>(Integer::compareTo, null, 2, false, null)
        ), getForwardDirection());
        assertEquals(8, queue.poll().getPolledItem().get().getItem().longValue());
        assertEquals(12, queue.poll().getPolledItem().get().getItem().longValue());
        assertEquals(15, queue.poll().getPolledItem().get().getItem().longValue());
    }
}
