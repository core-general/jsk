package sk.utils.collections.cluster_sorter.abstr;

import java.util.List;

public interface JskCsAbstract<SRC_ID, ITEM, EXPAND_DIRECTION> {
    List<ITEM> getNext(int count, EXPAND_DIRECTION direction);

    boolean hasNext(int initializingCount, EXPAND_DIRECTION direction);
}
