package sk.utils.collections.cluster_sorter.abstr.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.utils.collections.cluster_sorter.abstr.JskCsSource;
import sk.utils.functional.O;

@Getter
@AllArgsConstructor
public class JskCsPollResult<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE extends JskCsSource<SRC_ID, ITEM>> {
    private final O<JskCsItem<SRC_ID, ITEM, EXPAND_DIRECTION, SOURCE>> polledItem;
    private final EXPAND_DIRECTION direction;
}
