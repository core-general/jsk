package sk.utils.collections.cluster_sorter.abstr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.utils.collections.cluster_sorter.abstr.model.JcsSrcId;

@AllArgsConstructor
@Getter
public abstract class JcsASource<ITEM> implements JcsISource<ITEM> {
    protected JcsSrcId id;
}
