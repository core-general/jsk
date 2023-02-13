package sk.utils.collections.cluster_sorter.abstr.model;

import lombok.Value;
import sk.utils.ifaces.IdentifiableString;

@Value
public class JskCsSrcId implements IdentifiableString {
    String id;

    @Override
    public String toString() {
        return id;
    }
}
