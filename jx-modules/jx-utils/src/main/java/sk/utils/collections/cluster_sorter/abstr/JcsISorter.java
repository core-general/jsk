package sk.utils.collections.cluster_sorter.abstr;

import sk.utils.collections.cluster_sorter.abstr.model.JcsSrcId;

import java.util.List;
import java.util.Map;

public interface JcsISorter<ITEM, EXPAND_DIRECTION, SOURCE extends JcsISource<ITEM>> {
    List<ITEM> getNext(int count, EXPAND_DIRECTION direction);

    boolean hasNext(int initializingCount, EXPAND_DIRECTION direction);

    void setPositionToItem(ITEM item);

    Map<JcsSrcId, SOURCE> getAllSources();

    void addNewSource(SOURCE source);

    void removeSource(JcsSrcId id);
}
