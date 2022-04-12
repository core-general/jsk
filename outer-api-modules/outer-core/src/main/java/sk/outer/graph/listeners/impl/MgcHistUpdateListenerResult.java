package sk.outer.graph.listeners.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import sk.outer.graph.execution.MgcGraphHistoryItem;
import sk.outer.graph.listeners.MgcBaseOkListenerResult;

@AllArgsConstructor
@Data
public class MgcHistUpdateListenerResult extends MgcBaseOkListenerResult {
    MgcGraphHistoryItem nodeOrEdgeId;
}
