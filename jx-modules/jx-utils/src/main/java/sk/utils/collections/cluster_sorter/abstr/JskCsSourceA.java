package sk.utils.collections.cluster_sorter.abstr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.utils.collections.cluster_sorter.abstr.model.JskCsSrcId;

@AllArgsConstructor
@Getter
public abstract class JskCsSourceA<ITEM> implements JskCsSource<ITEM> {
    protected JskCsSrcId id;
}
