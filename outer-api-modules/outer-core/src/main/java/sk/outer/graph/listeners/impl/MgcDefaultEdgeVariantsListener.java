package sk.outer.graph.listeners.impl;

import lombok.AllArgsConstructor;
import sk.outer.graph.edges.MgcMetaEdge;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.listeners.MgcListener;
import sk.outer.graph.listeners.MgcListenerResult;
import sk.outer.graph.nodes.MgcNode;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class MgcDefaultEdgeVariantsListener implements MgcListener {
    public static final String id = "edge_possibilities";
    private MgcNode newNode;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public MgcListenerResult apply(MgcGraphExecutionContext context) {
        List<String> allPossibleEdges = context.getExecutedGraph().getAllEdgesFrom(newNode).stream()
                .filter($ -> !($ instanceof MgcMetaEdge))
                .flatMap($ -> $.getPossibleEdges($.getParsedData().getText(), context).stream())
                .collect(Collectors.toList());
        List<String> metaEdges = context.getExecutedGraph().getAllEdgesFrom(newNode).stream()
                .filter($ -> $ instanceof MgcMetaEdge)
                .flatMap($ -> $.getPossibleEdges($.getParsedData().getText(), context).stream())
                .collect(Collectors.toList());
        return new MgcEdgeVariantsListenerResult(allPossibleEdges, metaEdges);
    }
}
