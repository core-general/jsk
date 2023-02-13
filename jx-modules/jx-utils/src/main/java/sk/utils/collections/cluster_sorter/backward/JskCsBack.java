package sk.utils.collections.cluster_sorter.backward;

import sk.utils.collections.cluster_sorter.abstr.JskCsAbstract;
import sk.utils.functional.O;

import java.util.List;

public interface JskCsBack<ITEM, EXPAND_DIRECTION, SOURCE extends JskCsSourceBack<ITEM>>
        extends JskCsAbstract<ITEM, EXPAND_DIRECTION, SOURCE> {
    List<ITEM> getPrevious(int count);

    boolean hasPrevious(int initializingCount);

    O<ITEM> setPositionToItemAndReturnNearest(ITEM item);
}
