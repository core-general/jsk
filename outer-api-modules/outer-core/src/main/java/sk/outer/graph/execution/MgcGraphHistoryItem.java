package sk.outer.graph.execution;

import lombok.AllArgsConstructor;
import lombok.Data;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;

import java.util.List;

@Data
@AllArgsConstructor
public class MgcGraphHistoryItem {
    public static final String type = "sk.outer.graph.execution.MgcGraphHistoryItem";

    String graphId;
    String graphVersion;

    boolean node;
    String id;
    String text;
    List<String> possibleEdges;
    List<String> possibleMetaEdges;

    public boolean isId(String otherId) {
        return Fu.equal(id, otherId);
    }

    public List<String> getAllEdges() {
        return Cc.addAll(Cc.l(), possibleEdges, possibleMetaEdges);
    }
}
