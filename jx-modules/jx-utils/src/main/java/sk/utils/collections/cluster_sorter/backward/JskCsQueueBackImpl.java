package sk.utils.collections.cluster_sorter.backward;

import sk.utils.collections.cluster_sorter.abstr.JskCsQueueAbstractImpl;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsPollResult;
import sk.utils.collections.cluster_sorter.backward.model.JskCsBothType;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.*;

public class JskCsQueueBackImpl<SRC_ID, ITEM, SOURCE extends JskCsSourceBack<SRC_ID, ITEM, JskCsBothType>>
        extends JskCsQueueAbstractImpl<SRC_ID, ITEM, JskCsBothType, SOURCE>
        implements JskCsQueueBack<SRC_ID, ITEM, SOURCE> {
    protected List<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> forwardItems = new ArrayList<>();
    protected List<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> backItems = new ArrayList<>();

    public List<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> getForwardItems() {
        return Collections.unmodifiableList(forwardItems);
    }

    public List<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> getBackItems() {
        return Collections.unmodifiableList(backItems);
    }

    /**
     * Semantics of addAll are different compared to ForwardQueue - now we are adding new elements to items and backItems
     * according to current position (the
     * next item in items or next item in backItems). The newElements are split between items and backItems
     *
     * @param newData
     */
    @Override
    public void addAll(List<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> newData) {
        if (newData.size() == 0) {
            return;
        }

        List<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> newItemsSorted = Cc.sort(new ArrayList<>(newData));
        O<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> nextFromItems = Cc.last(forwardItems);

        Comparator<ITEM> itemsComparator = newItemsSorted.get(0).getComparator();

        int splitIndex = 0;

        for (; splitIndex < newItemsSorted.size(); splitIndex++) {
            JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE> newItem = newItemsSorted.get(splitIndex);
            boolean greaterThanFirstOfItems =
                    nextFromItems.map($ -> itemsComparator.compare(newItem.getItem(), $.getItem()) >= 0).orElse(true);

            if (greaterThanFirstOfItems) {
                break;
            }
        }

        List<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> toQueue = newItemsSorted.subList(splitIndex, newItemsSorted.size());
        List<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> toBackQueue = newItemsSorted.subList(0, splitIndex);

        putToQueues(toQueue, toBackQueue);
    }

    @Override
    public JskCsPollResult<SRC_ID, ITEM, JskCsBothType, SOURCE> poll(JskCsBothType jskCsBothType) {
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
    public Map<JskCsBothType, Iterator<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>>> getDirectionIterators() {
        return Cc.m(
                JskCsBothType.FORWARD, new JskCsItemIterator<>(forwardItems, () -> modCount),
                JskCsBothType.BACKWARD, new JskCsItemIterator<>(backItems, () -> modCount)
        );
    }


    @Override
    public O<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> setLastSelectedItemAndReturnLastUsed(ITEM newItem) {
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

    protected JskCsPollResult<SRC_ID, ITEM, JskCsBothType, SOURCE> uniBackPoll(
            List<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> source,
            List<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> target) {
        var polled = uniPoll(source, JskCsBothType.BACKWARD);
        polled.getPolledItem().ifPresent($ -> target.add($));
        return polled;
    }


    private void putToQueues(List<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> toQueue,
            List<JskCsItem<SRC_ID, ITEM, JskCsBothType, SOURCE>> toBackQueue) {
        uniAddAll(toQueue, forwardItems, item -> item.getComparator().reversed());
        uniAddAll(toBackQueue, backItems, item -> item.getComparator());
        modCount--;//compensation
    }
}
