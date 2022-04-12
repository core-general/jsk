package sk.outer.graph.listeners.impl;

import sk.outer.graph.edges.MgcEdge;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.execution.MgcGraphHistoryItem;
import sk.outer.graph.listeners.MgcListener;
import sk.outer.graph.listeners.MgcListenerResult;
import sk.outer.graph.nodes.MgcNode;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;

import static sk.outer.graph.listeners.impl.MgcDefaultNodeTextListener.id;

public class MgcDefaultHistoryUpdaterListener implements MgcListener {
    OneOf<MgcNode, MgcEdge> nodeOrEdge;

    private MgcDefaultHistoryUpdaterListener(OneOf<MgcNode, MgcEdge> nodeOrEdge) {
        this.nodeOrEdge = nodeOrEdge;
    }

    public static MgcDefaultHistoryUpdaterListener node(MgcNode nodeId) {
        return new MgcDefaultHistoryUpdaterListener(OneOf.left(nodeId));
    }

    public static MgcDefaultHistoryUpdaterListener edge(MgcEdge edgeId) {
        return new MgcDefaultHistoryUpdaterListener(OneOf.right(edgeId));
    }

    @Override
    public String getId() {
        return "history_updater";
    }

    @Override
    public MgcListenerResult apply(MgcGraphExecutionContext context) {
        MgcGraphHistoryItem item = new MgcGraphHistoryItem(context.getExecutedGraph().getId(),
                context.getExecutedGraph().getVersion(), nodeOrEdge.isLeft(),
                nodeOrEdge.collect($ -> $.getId(), $ -> $.getId()),
                nodeOrEdge.collect($ -> context.getNodeProcessor().getResultOf(id, MgcNodeTextListenerResult.class).getNewNode(),
                        $ -> context.getSelectedEdge()),
                nodeOrEdge.isLeft()
                ? context.getNodeProcessor()
                        .getResultOf(MgcDefaultEdgeVariantsListener.id, MgcEdgeVariantsListenerResult.class)
                        .getNormalEdges()
                : Cc.lEmpty(),
                nodeOrEdge.isLeft()
                ? context.getNodeProcessor()
                        .getResultOf(MgcDefaultEdgeVariantsListener.id, MgcEdgeVariantsListenerResult.class)
                        .getMetaEdges()
                : Cc.lEmpty());
        context.addGraphHistoryItem(item);
        return new MgcHistUpdateListenerResult(item);
    }
}
