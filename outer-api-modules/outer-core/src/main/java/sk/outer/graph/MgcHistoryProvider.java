package sk.outer.graph;

import sk.outer.graph.execution.MgcGraphHistoryItem;
import sk.utils.functional.O;

public interface MgcHistoryProvider {
    O<MgcGraphHistoryItem> currentGraphHistory(String user);
}
