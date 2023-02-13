package sk.utils.collections.cluster_sorter.backward;

import sk.utils.collections.cluster_sorter.abstr.JcsASorter;
import sk.utils.collections.cluster_sorter.abstr.JcsIExpandElementsStrategy;
import sk.utils.collections.cluster_sorter.abstr.JcsInitStrategy;
import sk.utils.collections.cluster_sorter.backward.model.JcsEBackType;

import java.util.Comparator;
import java.util.List;

/**
 * We cache values which we already polled and return them back if we go back
 */
public class JcsSorterBackSimple<ITEM, SOURCE extends JcsISourceBack<ITEM>>
        extends JcsASorter<ITEM, JcsEBackType, JcsIQueueBack<ITEM, SOURCE>, SOURCE>
        implements JcsISorterBack<ITEM, JcsEBackType, SOURCE> {

    private JcsSorterBackSimple(
            List<SOURCE> sources,
            Comparator<ITEM> comparator,
            JcsInitStrategy<ITEM, JcsEBackType, SOURCE> firstFeedStrategy,
            JcsIExpandElementsStrategy<ITEM, JcsEBackType, SOURCE> getMoreStrategy
    ) {
        super(sources, comparator, firstFeedStrategy, getMoreStrategy);
    }

    @Override
    public List<ITEM> getPrevious(int count) {
        return traverseQueue(count, () -> queue.pollBack());
    }

    @Override
    public boolean hasPrevious(int initializingCount) {
        initIfNeeded(initializingCount);
        return queue.iteratorBack().hasNext();
    }

    @Override
    protected JcsIQueueBack<ITEM, SOURCE> instantiateQueue() {
        return new JcsQueueBack<>();
    }
}
