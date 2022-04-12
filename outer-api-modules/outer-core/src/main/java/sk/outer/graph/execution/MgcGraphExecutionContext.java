package sk.outer.graph.execution;

import sk.outer.graph.listeners.MgcListenerProcessorResultImpl;
import sk.outer.graph.nodes.MgcGraph;
import sk.outer.graph.nodes.MgcNode;
import sk.utils.functional.O;
import sk.utils.paging.SimplePage;

import java.util.List;

public interface MgcGraphExecutionContext {
    default List<MgcGraphHistoryItem> getGraphHistoryDescending(int lastX) {
        return getGraphHistory(lastX, O.empty(), false, O.empty()).getData();
    }

    default List<MgcGraphHistoryItem> getGraphHistoryDescending(int lastX, boolean isNode) {
        return getGraphHistory(lastX, O.empty(), false, O.of(isNode)).getData();
    }

    SimplePage<MgcGraphHistoryItem, String> getGraphHistory(int count, O<String> npa,
            boolean ascending, O<Boolean> isNodeOrAll);

    MgcGraph getExecutedGraph();

    MgcNode getFromNode();

    MgcNode getToNode();

    void setToNode(MgcNode toNode);

    String getSelectedEdge();

    void addGraphHistoryItem(MgcGraphHistoryItem item);

    MgcListenerProcessorResultImpl getEdgeProcessor();

    MgcListenerProcessorResultImpl getNodeProcessor();
}
