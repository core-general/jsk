package sk.utils.collections.cluster_sorter.backward;

import sk.utils.collections.cluster_sorter.abstr.JskCsAbstractImpl;
import sk.utils.collections.cluster_sorter.abstr.JskCsExpandElementsStrategy;
import sk.utils.collections.cluster_sorter.abstr.JskCsInitStrategy;
import sk.utils.collections.cluster_sorter.backward.model.JskCsBackType;
import sk.utils.functional.O;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * We cache values which we already polled and return them back if we go back
 */
public class JskCsBackSimpleImpl<ITEM, SOURCE extends JskCsSourceBack<ITEM>>
        extends JskCsAbstractImpl<ITEM, JskCsBackType, JskCsQueueBack<ITEM, SOURCE>, SOURCE>
        implements JskCsBack<ITEM, JskCsBackType, SOURCE> {

    private JskCsBackSimpleImpl(
            List<SOURCE> sources,
            Comparator<ITEM> comparator,
            JskCsInitStrategy<ITEM, JskCsBackType, SOURCE> firstFeedStrategy,
            JskCsExpandElementsStrategy<ITEM, JskCsBackType, SOURCE> getMoreStrategy
    ) {
        super(sources, comparator, firstFeedStrategy, getMoreStrategy);
    }


    @Override
    public List<ITEM> getPrevious(int count) {
        initIfNeeded(count);

        List<ITEM> toRet = new ArrayList<>();
        traverseQueue(toRet, count, () -> queue.pollBack());
        return toRet;
    }

    @Override
    public boolean hasPrevious(int initializingCount) {
        initIfNeeded(initializingCount);

        return queue.iteratorBack().hasNext();
    }

    @Override
    public O<ITEM> setPositionToItemAndReturnNearest(ITEM item) {
        return O.empty();//TODO !!!!!!!!!!!!!!!!!!!...
    }

    @Override
    protected JskCsQueueBack<ITEM, SOURCE> instantiateQueue() {
        return new JskCsQueueBackImpl<>();
    }
}
