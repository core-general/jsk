package sk.utils.collections.cluster_sorter.forward;

import sk.utils.collections.cluster_sorter.abstr.JcsISorter;
import sk.utils.collections.cluster_sorter.abstr.JcsISource;
import sk.utils.collections.cluster_sorter.forward.model.JcsEForwardType;

import java.util.List;

public interface JcsISorterForward<ITEM, SOURCE extends JcsISource<ITEM>>
        extends JcsISorter<ITEM, JcsEForwardType, SOURCE> {
    default List<ITEM> getNext(int count) {
        return getNext(count, JcsEForwardType.FORWARD);
    }

    default boolean hasNext(int initializingCount) {
        return hasNext(initializingCount, JcsEForwardType.FORWARD);
    }
}
