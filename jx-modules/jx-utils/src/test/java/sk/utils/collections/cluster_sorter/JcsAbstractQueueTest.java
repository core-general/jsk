package sk.utils.collections.cluster_sorter;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import sk.utils.collections.cluster_sorter.abstr.JcsIQueue;
import sk.utils.collections.cluster_sorter.abstr.model.JcsItem;
import sk.utils.statics.Cc;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public abstract class JcsAbstractQueueTest<EXPAND_DIRECTION, QUEUE extends JcsIQueue<Integer, EXPAND_DIRECTION, ?>> {
    protected QUEUE queue;

    protected abstract QUEUE initQueue();

    protected abstract EXPAND_DIRECTION getForwardDirection();

    @Before
    public void init() {
        queue = initQueue();
        queue.addAllRespectConsumed(Cc.l(
                new JcsItem<>(Integer::compareTo, null, 1, false, null),
                new JcsItem<>(Integer::compareTo, null, 5, false, null),
                new JcsItem<>(Integer::compareTo, null, 3, false, null)
        ));
        queue.addAllRespectConsumed(Cc.l(
                new JcsItem<>(Integer::compareTo, null, 2, false, null),
                new JcsItem<>(Integer::compareTo, null, 7, false, null),
                new JcsItem<>(Integer::compareTo, null, 1, false, null)
        ));
    }

    @Test
    public void iterator() {
        {
            List<Integer> values = Cc.l();

            Iterator<JcsItem<Integer, EXPAND_DIRECTION, ?>> iterator =
                    (Iterator<JcsItem<Integer, EXPAND_DIRECTION, ?>>) (Object) queue.getDirectionIterators()
                            .get(getForwardDirection());
            for (JcsItem<Integer, EXPAND_DIRECTION, ?> item : new Iterable<JcsItem<Integer, EXPAND_DIRECTION, ?>>() {
                @NotNull
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
