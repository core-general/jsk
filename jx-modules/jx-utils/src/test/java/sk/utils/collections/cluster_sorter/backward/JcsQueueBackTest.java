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


import org.junit.jupiter.api.Test;
import sk.utils.collections.cluster_sorter.JcsAbstractQueueTest;
import sk.utils.collections.cluster_sorter.abstr.model.JcsItem;
import sk.utils.collections.cluster_sorter.backward.impl.JcsQueueBack;
import sk.utils.collections.cluster_sorter.backward.model.JcsEBackType;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JcsQueueBackTest extends JcsAbstractQueueTest<JcsEBackType, JcsQueueBack<Integer, ?>> {
    @Override
    protected JcsQueueBack<Integer, ?> initQueue() {
        return new JcsQueueBack<>();
    }

    @Override
    protected JcsEBackType getForwardDirection() {
        return JcsEBackType.FORWARD;
    }

    @Test
    public void addAll() {
        assertEquals("7,5,3,2,1,1", format(queue.getForwardItems()));
        assertEquals("", format(queue.getBackItems()));

        queue.poll();
        queue.poll();
        queue.poll();

        assertEquals("7,5,3", format(queue.getForwardItems()));
        assertEquals("1,1,2", format(queue.getBackItems()));

        queue.addAllRespectConsumed(Cc.l(
                new JcsItem<>(Integer::compareTo, null, 4, false, null),
                new JcsItem<>(Integer::compareTo, null, 1, false, null),
                new JcsItem<>(Integer::compareTo, null, 0, false, null),
                new JcsItem<>(Integer::compareTo, null, 3, false, null)
        ), getForwardDirection());

        assertEquals("7,5,4,3,3", format(queue.getForwardItems()));
        assertEquals("0,1,1,1,2", format(queue.getBackItems()));

        queue.poll();
        assertEquals("7,5,4,3", format(queue.getForwardItems()));
        assertEquals("0,1,1,1,2,3", format(queue.getBackItems()));

        queue.pollBack();
        queue.pollBack();

        assertEquals("7,5,4,3,3,2", format(queue.getForwardItems()));
        assertEquals("0,1,1,1", format(queue.getBackItems()));

        for (int i = 0; i < 5; i++) {
            queue.pollBack();
        }
        assertEquals(queue.pollBack().getPolledItem(), O.empty());
        assertEquals("7,5,4,3,3,2,1,1,1,0", format(queue.getForwardItems()));
        assertEquals("", format(queue.getBackItems()));

        for (int i = 0; i < 10; i++) {
            queue.poll();
        }
        assertEquals(queue.poll().getPolledItem(), O.empty());
        assertEquals("", format(queue.getForwardItems()));
        assertEquals("0,1,1,1,2,3,3,4,5,7", format(queue.getBackItems()));
    }

    @Test
    public void removeIfTest() {
        assertEquals("7,5,3,2,1,1", format(queue.getForwardItems()));
        assertEquals("", format(queue.getBackItems()));

        assertEquals("7", format(queue.removeElementsIf(item -> item.getItem() > 5)));
        assertEquals("5,3,2,1,1", format(queue.getForwardItems()));
        assertEquals("", format(queue.getBackItems()));

        queue.poll();
        queue.poll();
        queue.poll();
        assertEquals("5,3", format(queue.getForwardItems()));
        assertEquals("1,1,2", format(queue.getBackItems()));

        assertEquals("5,3,1,1", format(queue.removeElementsIf(item -> item.getItem() % 2 == 1)));
        assertEquals("", format(queue.getForwardItems()));
        assertEquals("2", format(queue.getBackItems()));

        assertEquals(O.empty(), queue.poll().getPolledItem());
        assertEquals(2, queue.pollBack().getPolledItem().get().getItem().intValue());
        assertEquals("2", format(queue.getForwardItems()));
        assertEquals("", format(queue.getBackItems()));
    }

    @Test
    public void iteratorBackTest() {
        assertEquals("7,5,3,2,1,1", format(queue.getForwardItems()));
        assertEquals("", format(queue.getBackItems()));
        assertEquals("",
                format(Cc.list(() -> (Iterator<JcsItem<Integer, JcsEBackType, ?>>) (Object) queue.iteratorBack())));
        queue.poll();
        queue.poll();
        queue.poll();
        queue.poll();
        assertEquals("3,2,1,1",
                format(Cc.list(() -> (Iterator<JcsItem<Integer, JcsEBackType, ?>>) (Object) queue.iteratorBack())));
    }

    @Test
    public void setLastSelectedItemTest() {
        assertEquals("7,5,3,2,1,1", format(queue.getForwardItems()));
        assertEquals("", format(queue.getBackItems()));

        assertEquals(2, queue.setLastSelectedItemAndReturnLastUsed(2).get().getItem().intValue());
        assertEquals("7,5,3", format(queue.getForwardItems()));
        assertEquals("1,1,2", format(queue.getBackItems()));

        assertEquals(7, queue.setLastSelectedItemAndReturnLastUsed(7).get().getItem().intValue());
        assertEquals("", format(queue.getForwardItems()));
        assertEquals("1,1,2,3,5,7", format(queue.getBackItems()));

        assertEquals(O.empty(), queue.setLastSelectedItemAndReturnLastUsed(1));
        assertEquals("7,5,3,2,1,1", format(queue.getForwardItems()));
        assertEquals("", format(queue.getBackItems()));

        assertEquals(5, queue.setLastSelectedItemAndReturnLastUsed(5).get().getItem().intValue());
        assertEquals("7", format(queue.getForwardItems()));
        assertEquals("1,1,2,3,5", format(queue.getBackItems()));
    }


    private String format(List<? extends JcsItem<Integer, JcsEBackType, ?>> items) {
        return Cc.join(items.stream().map($ -> $.getItem() + ""));
    }
}
