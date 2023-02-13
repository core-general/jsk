package sk.utils.collections.cluster_sorter.forward;

import sk.utils.collections.cluster_sorter.abstr.JskCsAbstract;
import sk.utils.collections.cluster_sorter.abstr.JskCsSource;
import sk.utils.collections.cluster_sorter.forward.model.JskCsForwardType;

import java.util.List;

public interface JskCsForward<ITEM, SOURCE extends JskCsSource<ITEM>>
        extends JskCsAbstract<ITEM, JskCsForwardType, SOURCE> {
    default List<ITEM> getNext(int count) {
        return getNext(count, JskCsForwardType.FORWARD);
    }

    default boolean hasNext(int initializingCount) {
        return hasNext(initializingCount, JskCsForwardType.FORWARD);
    }
}
