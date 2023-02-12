package sk.utils.collections.cluster_sorter.backward;

import sk.utils.collections.cluster_sorter.abstr.JskCsAbstractImpl;
import sk.utils.collections.cluster_sorter.abstr.JskCsExpandElementsStrategy;
import sk.utils.collections.cluster_sorter.abstr.JskCsInitStrategy;
import sk.utils.collections.cluster_sorter.backward.model.JskCsBothType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * We cache values which we already polled and return them back if we go back
 *
 * @param <SRC_ID>
 * @param <ITEM>
 */
public class JskCsBackSimpleImpl<SRC_ID, ITEM, SOURCE extends JskCsSourceBack<SRC_ID, ITEM, JskCsBothType>>
        extends JskCsAbstractImpl<SRC_ID, ITEM, JskCsBothType, JskCsQueueBack<SRC_ID, ITEM, SOURCE>, SOURCE>
        implements JskCsBack<SRC_ID, ITEM, JskCsBothType> {

    private JskCsBackSimpleImpl(
            List<SOURCE> sources,
            Comparator<ITEM> comparator,
            JskCsInitStrategy<SRC_ID, ITEM, JskCsBothType, SOURCE> firstFeedStrategy,
            JskCsExpandElementsStrategy<SRC_ID, ITEM, JskCsBothType, SOURCE> getMoreStrategy
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
    protected JskCsQueueBack<SRC_ID, ITEM, SOURCE> instantiateQueue() {
        return new JskCsQueueBackImpl<>();
    }
}
