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

import org.junit.Before;
import org.junit.Test;
import sk.utils.collections.cluster_sorter.model.JskCsItem;
import sk.utils.statics.Cc;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class JskCsQueueImplTest {
    JskCsQueueImpl<?, Integer> queue;

    @Before
    public void init() {
        queue = new JskCsQueueImpl<>();
        queue.addAll(Cc.l(
                new JskCsItem<>(Integer::compareTo, null, 1, false),
                new JskCsItem<>(Integer::compareTo, null, 5, false),
                new JskCsItem<>(Integer::compareTo, null, 3, false)
        ));
        queue.addAll(Cc.l(
                new JskCsItem<>(Integer::compareTo, null, 2, false),
                new JskCsItem<>(Integer::compareTo, null, 7, false),
                new JskCsItem<>(Integer::compareTo, null, 1, false)
        ));
    }

    @Test
    public void addAll() {
        assertEquals("7,5,3,2,1,1", Cc.join(queue.items.stream().map($ -> $.getItem() + "")));
    }

    @Test
    public void poll() {
        assertEquals(1, queue.poll().get().getItem().longValue());
        assertEquals(1, queue.poll().get().getItem().longValue());
        assertEquals(2, queue.poll().get().getItem().longValue());

        queue.addAll(Cc.l(
                new JskCsItem<>(Integer::compareTo, null, 1, false),
                new JskCsItem<>(Integer::compareTo, null, 8, false),
                new JskCsItem<>(Integer::compareTo, null, 2, false)
        ));

        assertEquals(1, queue.poll().get().getItem().longValue());
        assertEquals(2, queue.poll().get().getItem().longValue());
        assertEquals(3, queue.poll().get().getItem().longValue());
        assertEquals(5, queue.poll().get().getItem().longValue());
        assertEquals(7, queue.poll().get().getItem().longValue());
        assertEquals(8, queue.poll().get().getItem().longValue());
    }

    @Test
    public void iterator() {
        List<Integer> values = Cc.l();
        for (JskCsItem<?, Integer> item : queue) {
            values.add(item.getItem());
        }
        assertEquals("1,1,2,3,5,7", Cc.join(values));

        final Iterator<? extends JskCsItem<?, Integer>> iterator = queue.iterator();
        iterator.next();
        queue.poll();
        assertThrows(ConcurrentModificationException.class, () -> iterator.next());
    }
}
