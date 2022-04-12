package sk.outer.graph.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import sk.utils.statics.Fu;

import java.util.List;

@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Data
public class MgcParsedData {
    String id;
    String type;
    List<String> params;
    String text;

    public boolean typeOrParamsContains(String contain, boolean strict) {
        return strict
               ? Fu.equal(type, contain) || params.stream().anyMatch($ -> Fu.equal($, contain))
               : type.contains(contain) || params.stream().anyMatch($ -> $.contains(contain));
    }
}
