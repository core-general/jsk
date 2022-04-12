package sk.outer.graph.listeners.impl;

import lombok.Data;
import sk.outer.graph.listeners.MgcBaseOkListenerResult;
import sk.utils.statics.Cc;

import java.util.List;

@Data
public class MgcEdgeVariantsListenerResult extends MgcBaseOkListenerResult {
    final List<String> normalEdges;
    final List<String> metaEdges;

    public List<String> getAllEdges() {
        return Cc.addAll(Cc.l(), normalEdges, metaEdges);
    }
}
