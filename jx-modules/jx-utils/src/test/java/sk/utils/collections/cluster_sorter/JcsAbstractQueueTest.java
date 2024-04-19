package sk.utils.collections.cluster_sorter;

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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sk.utils.collections.cluster_sorter.abstr.JcsIQueue;
import sk.utils.collections.cluster_sorter.abstr.model.JcsItem;
import sk.utils.statics.Cc;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class JcsAbstractQueueTest<EXPAND_DIRECTION, QUEUE extends JcsIQueue<Integer, EXPAND_DIRECTION, ?>> {
    protected QUEUE queue;

    protected abstract QUEUE initQueue();

    protected abstract EXPAND_DIRECTION getForwardDirection();

    @BeforeEach
    public void init() {
        queue = initQueue();
        queue.addAllRespectConsumed(Cc.l(
                new JcsItem<>(Integer::compareTo, null, 1, false, null),
                new JcsItem<>(Integer::compareTo, null, 5, false, null),
                new JcsItem<>(Integer::compareTo, null, 3, false, null)
        ), getForwardDirection());
        queue.addAllRespectConsumed(Cc.l(
                new JcsItem<>(Integer::compareTo, null, 2, false, null),
                new JcsItem<>(Integer::compareTo, null, 7, false, null),
                new JcsItem<>(Integer::compareTo, null, 1, false, null)
        ), getForwardDirection());
    }

    @Test
    public void iterator() {
        {
            List<Integer> values = Cc.l();

            Iterator<JcsItem<Integer, EXPAND_DIRECTION, ?>> iterator =
                    (Iterator<JcsItem<Integer, EXPAND_DIRECTION, ?>>) (Object) queue.getDirectionIterators()
                            .get(getForwardDirection());
            for (JcsItem<Integer, EXPAND_DIRECTION, ?> item : new Iterable<JcsItem<Integer, EXPAND_DIRECTION, ?>>() {

                @Override
                public Iterator<JcsItem<Integer, EXPAND_DIRECTION, ?>> iterator() {
                    return iterator;
                }
            }) {
                values.add(item.getItem());
            }
            assertEquals("1,1,2,3,5,7", Cc.join(values));
        }

        {
            final Iterator<? extends JcsItem<Integer, EXPAND_DIRECTION, ?>> it =
                    queue.getDirectionIterators().get(getForwardDirection());
            it.next();
            queue.poll(getForwardDirection());
            assertThrows(ConcurrentModificationException.class, () -> it.next());
        }
    }
}
