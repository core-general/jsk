package sk.utils.collections.cluster_sorter.backward;

import sk.utils.collections.cluster_sorter.abstr.JskCsAbstract;

import java.util.List;

public interface JskCsBack<SRC_ID, ITEM, EXPAND_DIRECTION> extends JskCsAbstract<SRC_ID, ITEM, EXPAND_DIRECTION> {
    List<ITEM> getPrevious(int count);

    boolean hasPrevious(int initializingCount);
}
