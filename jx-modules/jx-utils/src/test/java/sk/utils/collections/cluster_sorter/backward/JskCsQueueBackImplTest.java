package sk.utils.collections.cluster_sorter.backward;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import sk.utils.collections.cluster_sorter.JskCsAbstractQueueTest;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsItem;
import sk.utils.collections.cluster_sorter.backward.model.JskCsBackType;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JskCsQueueBackImplTest extends JskCsAbstractQueueTest<JskCsBackType, JskCsQueueBackImpl<Integer, ?>> {
    @Override
    protected JskCsQueueBackImpl<Integer, ?> initQueue() {
        return new JskCsQueueBackImpl<>();
    }

    @Override
    protected JskCsBackType getForwardDirection() {
        return JskCsBackType.FORWARD;
    }

    @Test
    public void addAll() {
        JskCsQueueBackImpl<Integer, ?> queue1 = (JskCsQueueBackImpl<Integer, ?>) queue;
        assertEquals("7,5,3,2,1,1", format(queue1.getForwardItems()));
        assertEquals("", format(queue1.getBackItems()));

        queue1.poll();
        queue1.poll();
        queue1.poll();

        assertEquals("7,5,3", format(queue1.getForwardItems()));
        assertEquals("1,1,2", format(queue1.getBackItems()));

        queue1.addAllRespectConsumed(Cc.l(
                new JskCsItem<>(Integer::compareTo, null, 4, false, null),
                new JskCsItem<>(Integer::compareTo, null, 1, false, null),
                new JskCsItem<>(Integer::compareTo, null, 0, false, null),
                new JskCsItem<>(Integer::compareTo, null, 3, false, null)
        ));

        assertEquals("7,5,4,3,3", format(queue1.getForwardItems()));
        assertEquals("0,1,1,1,2", format(queue1.getBackItems()));

        queue1.poll();
        assertEquals("7,5,4,3", format(queue1.getForwardItems()));
        assertEquals("0,1,1,1,2,3", format(queue1.getBackItems()));

        queue1.pollBack();
        queue1.pollBack();

        assertEquals("7,5,4,3,3,2", format(queue1.getForwardItems()));
        assertEquals("0,1,1,1", format(queue1.getBackItems()));

        for (int i = 0; i < 5; i++) {
            queue1.pollBack();
        }
        assertEquals(queue1.pollBack().getPolledItem(), O.empty());
        assertEquals("7,5,4,3,3,2,1,1,1,0", format(queue1.getForwardItems()));
        assertEquals("", format(queue1.getBackItems()));

        for (int i = 0; i < 10; i++) {
            queue1.poll();
        }
        assertEquals(queue1.poll().getPolledItem(), O.empty());
        assertEquals("", format(queue1.getForwardItems()));
        assertEquals("0,1,1,1,2,3,3,4,5,7", format(queue1.getBackItems()));
    }

    @Test
    public void iteratorBackTest() {
        JskCsQueueBackImpl<Integer, ?> queue1 = (JskCsQueueBackImpl<Integer, ?>) queue;
        assertEquals("7,5,3,2,1,1", format(queue1.getForwardItems()));
        assertEquals("", format(queue1.getBackItems()));
        assertEquals("",
                format(Cc.list(() -> (Iterator<JskCsItem<Integer, JskCsBackType, ?>>) (Object) queue1.iteratorBack())));
        queue1.poll();
        queue1.poll();
        queue1.poll();
        queue1.poll();
        assertEquals("3,2,1,1",
                format(Cc.list(() -> (Iterator<JskCsItem<Integer, JskCsBackType, ?>>) (Object) queue1.iteratorBack())));
    }

    @Test
    public void setLastSelectedItemTest() {
        JskCsQueueBackImpl<Integer, ?> queue1 = (JskCsQueueBackImpl<Integer, ?>) queue;
        assertEquals("7,5,3,2,1,1", format(queue1.getForwardItems()));
        assertEquals("", format(queue1.getBackItems()));

        assertEquals(2, queue1.setLastSelectedItemAndReturnLastUsed(2).get().getItem().intValue());
        assertEquals("7,5,3", format(queue1.getForwardItems()));
        assertEquals("1,1,2", format(queue1.getBackItems()));

        assertEquals(7, queue1.setLastSelectedItemAndReturnLastUsed(7).get().getItem().intValue());
        assertEquals("", format(queue1.getForwardItems()));
        assertEquals("1,1,2,3,5,7", format(queue1.getBackItems()));

        assertEquals(O.empty(), queue1.setLastSelectedItemAndReturnLastUsed(1));
        assertEquals("7,5,3,2,1,1", format(queue1.getForwardItems()));
        assertEquals("", format(queue1.getBackItems()));

        assertEquals(5, queue1.setLastSelectedItemAndReturnLastUsed(5).get().getItem().intValue());
        assertEquals("7", format(queue1.getForwardItems()));
        assertEquals("1,1,2,3,5", format(queue1.getBackItems()));
    }

    @NotNull
    private String format(List<? extends JskCsItem<Integer, JskCsBackType, ?>> items) {
        return Cc.join(items.stream().map($ -> $.getItem() + ""));
    }
}