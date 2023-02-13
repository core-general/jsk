package sk.utils.collections.cluster_sorter.backward;

import sk.utils.collections.cluster_sorter.abstr.JskCsQueueAbstractImpl;
import sk.utils.collections.cluster_sorter.abstr.JskCsSource;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsItem;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsPollResult;
import sk.utils.collections.cluster_sorter.backward.model.JskCsBackType;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.*;

public class JskCsQueueBackImpl<ITEM, SOURCE extends JskCsSource<ITEM>>
        extends JskCsQueueAbstractImpl<ITEM, JskCsBackType, SOURCE>
        implements JskCsQueueBack<ITEM, SOURCE> {
    protected List<JskCsItem<ITEM, JskCsBackType, SOURCE>> forwardItems = new ArrayList<>();
    protected List<JskCsItem<ITEM, JskCsBackType, SOURCE>> backItems = new ArrayList<>();

    public List<JskCsItem<ITEM, JskCsBackType, SOURCE>> getForwardItems() {
        return Collections.unmodifiableList(forwardItems);
    }

    public List<JskCsItem<ITEM, JskCsBackType, SOURCE>> getBackItems() {
        return Collections.unmodifiableList(backItems);
    }

    @Override
    public O<JskCsItem<ITEM, JskCsBackType, SOURCE>> getLastConsumedItem() {
        return Cc.last(backItems);
    }

    @Override
    public JskCsPollResult<ITEM, JskCsBackType, SOURCE> poll(JskCsBackType jskCsBothType) {
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
    public Map<JskCsBackType, Iterator<JskCsItem<ITEM, JskCsBackType, SOURCE>>> getDirectionIterators() {
        return Cc.m(
                JskCsBackType.FORWARD, new JskCsItemIterator<>(forwardItems, () -> modCount),
                JskCsBackType.BACKWARD, new JskCsItemIterator<>(backItems, () -> modCount)
        );
    }


    @Override
    public O<JskCsItem<ITEM, JskCsBackType, SOURCE>> setLastSelectedItemAndReturnLastUsed(ITEM newItem) {
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

    protected JskCsPollResult<ITEM, JskCsBackType, SOURCE> uniBackPoll(
            List<JskCsItem<ITEM, JskCsBackType, SOURCE>> source,
            List<JskCsItem<ITEM, JskCsBackType, SOURCE>> target) {
        var polled = uniPoll(source, JskCsBackType.BACKWARD);
        polled.getPolledItem().ifPresent($ -> target.add($));
        return polled;
    }

    @Override
    public void onDidNotGetToMainQueueWhenAddRespectOrder(List<JskCsItem<ITEM, JskCsBackType, SOURCE>> items) {
        uniAddAll(items, backItems, item -> item.getComparator());
    }

    @Override
    protected List<JskCsItem<ITEM, JskCsBackType, SOURCE>> getQueuePartToAddElements() {
        return forwardItems;
    }
}
