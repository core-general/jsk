package sk.utils.collections.cluster_sorter.abstr.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.utils.collections.cluster_sorter.abstr.JcsISource;
import sk.utils.functional.O;

@Getter
@AllArgsConstructor
public class JcsPollResult<ITEM, EXPAND_DIRECTION, SOURCE extends JcsISource<ITEM>> {
    private final O<JcsItem<ITEM, EXPAND_DIRECTION, SOURCE>> polledItem;
    private final EXPAND_DIRECTION direction;
}
