package sk.utils.collections.cluster_sorter.abstr;

import sk.utils.collections.cluster_sorter.abstr.model.JskCsSrcId;

import java.util.List;
import java.util.Map;

public interface JskCsAbstract<ITEM, EXPAND_DIRECTION, SOURCE extends JskCsSource<ITEM>> {
    List<ITEM> getNext(int count, EXPAND_DIRECTION direction);

    boolean hasNext(int initializingCount, EXPAND_DIRECTION direction);

    Map<JskCsSrcId, SOURCE> getAllSources();

    void addNewSource(SOURCE source);

    void removeSource(JskCsSrcId id);
}
