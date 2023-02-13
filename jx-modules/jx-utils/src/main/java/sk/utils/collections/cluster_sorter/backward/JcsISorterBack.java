package sk.utils.collections.cluster_sorter.backward;

import sk.utils.collections.cluster_sorter.abstr.JcsISorter;
import sk.utils.functional.O;

import java.util.List;

public interface JcsISorterBack<ITEM, EXPAND_DIRECTION, SOURCE extends JcsISourceBack<ITEM>>
        extends JcsISorter<ITEM, EXPAND_DIRECTION, SOURCE> {
    List<ITEM> getPrevious(int count);

    boolean hasPrevious(int initializingCount);

    O<ITEM> setPositionToItemAndReturnNearest(ITEM item);
}
