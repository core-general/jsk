package sk.outer.graph.nodes;

import sk.outer.graph.edges.MgcEdge;
import sk.outer.graph.edges.MgcMetaEdge;
import sk.outer.graph.edges.MgcNormalEdge;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.execution.MgcGraphExecutionResult;
import sk.outer.graph.execution.MgcGraphExecutionResultImpl;
import sk.outer.graph.listeners.MgcListenerProcessorResultImpl;
import sk.outer.graph.parser.MgcGraphExecutionContextGenerator;
import sk.utils.functional.F2;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static sk.utils.functional.O.ofNullable;

public interface MgcGraph extends MgcNode {
    String getVersion();

    boolean addNormalEdge(MgcNode from, MgcNode to, MgcNormalEdge edge);

    boolean addMetaEdge(MgcNode to, MgcMetaEdge metaEdge);

    boolean addMetaEdgeBack(MgcNode from, MgcMetaEdge metaEdge);

    boolean addNode(MgcNode node);

    List<MgcNode> getAllNodes();

    O<MgcNode> getNodeById(String nodeId);

    O<MgcEdge> getEdgeById(String edgeId);

    MgcNode toNode(MgcEdge nextEdge, MgcGraphExecutionContext context);

    default List<MgcEdge> getAllEdgesFrom() {
        return Cc.addAll(getAllDirectEdgesFrom(),
                new ArrayList<>(getMetaEdges()),
                new ArrayList<>(getMetaEdgesBack()),
                new ArrayList<>(getParentMetaEdges())
        );
    }


    default List<MgcEdge> getAllDirectEdgesFrom() {
        return getAllNodes().stream().flatMap($ -> getDirectEdgesFrom($).stream()).distinct().collect(Cc.toL());
    }

    List<MgcNormalEdge> getDirectEdgesFrom(MgcNode node);

    List<MgcNormalEdge> getDirectEdgesTo(MgcNode node);

    List<MgcMetaEdge> getMetaEdges();

    List<MgcEdge> getMetaEdgesBack();

    O<MgcGraph> getParent();

    default List<MgcMetaEdge> getParentMetaEdges() {
        return getParent().map($ -> Cc.addAll($.getMetaEdges(), $.getParentMetaEdges())).orElseGet(Cc::l);
    }

    default List<MgcEdge> getAllEdgesFrom(MgcNode node) {
        List<MgcEdge> edges = Cc.l();
        edges.addAll(getDirectEdgesFrom(node));
        edges.addAll(getMetaEdges());
        edges.addAll(getMetaEdgesBack(node));
        edges.addAll(getParentMetaEdges());
        return edges;
    }

    List<MgcMetaEdge> getMetaEdgesBack(MgcNode node);

    MgcGraphExecutionResult instantiateResult(
            MgcListenerProcessorResultImpl edgeResult, MgcListenerProcessorResultImpl nodeResult,
            MgcGraphExecutionContext context);

    default MgcGraphExecutionResult execute(MgcNode currentNode, String selectedEdge,
            MgcGraphExecutionContextGenerator contextProvider) {
        MgcGraphExecutionContext context = contextProvider.apply(this, currentNode, selectedEdge);
        return searchNextEdge(currentNode, selectedEdge, context)
                .map($ -> privateExecute($, context))
                .orElseGet(() -> new MgcGraphExecutionResultImpl(context, true));
    }

    default MgcGraphExecutionResult execute(MgcNode currentNode, MgcEdge selectedEdge,
            MgcGraphExecutionContextGenerator contextProvider) {
        MgcGraphExecutionContext context = contextProvider.apply(this, currentNode, selectedEdge.getId());
        return privateExecute(selectedEdge, context);
    }

    default MgcGraphExecutionResult executeFirst(MgcNode currentNode, MgcGraphExecutionContextGenerator contextProvider) {
        MgcGraphExecutionContext context = contextProvider.apply(this, currentNode, null);
        return privateFirstExecute(currentNode, context);
    }

    default O<MgcEdge> searchNextEdge(MgcNode currentNode, String selectedEdge, MgcGraphExecutionContext context) {
        F2<List<? extends MgcEdge>, Supplier<MgcEdge>, MgcEdge> processor =
                (mgcEdges, supplier) -> mgcEdges.stream()
                        .filter($ -> $.acceptEdge(selectedEdge, context))
                        .map($ -> (MgcEdge) $).findFirst()
                        .orElseGet(supplier);

        MgcEdge edge = processor.apply(getParentMetaEdges(),
                () -> processor.apply(getMetaEdges(),
                        () -> processor.apply(getMetaEdgesBack(currentNode),
                                () -> processor.apply(getDirectEdgesFrom(currentNode), () -> null))));


        return ofNullable(edge);
    }

    default MgcGraphExecutionResult privateExecute(MgcEdge nextEdge, MgcGraphExecutionContext context) {
        MgcNode to = toNode(nextEdge, context);
        context.setToNode(to);
        MgcListenerProcessorResultImpl edgeExecution = nextEdge.executeListeners(context, context.getEdgeProcessor());
        MgcListenerProcessorResultImpl nodeExecution = to.executeListeners(context, context.getNodeProcessor());
        return instantiateResult(edgeExecution, nodeExecution, context);
    }

    default MgcGraphExecutionResult privateFirstExecute(MgcNode initial, MgcGraphExecutionContext context) {
        context.setToNode(initial);
        MgcListenerProcessorResultImpl nodeExecution = initial.executeListeners(context, context.getNodeProcessor());
        return instantiateResult(new MgcListenerProcessorResultImpl(), nodeExecution, context);
    }

    MgcNode getEdgeSource(MgcEdge $);

    MgcNode getEdgeTarget(MgcEdge $);

    String getStartingStateId();

    Set<String> getFinishStateIds();

    O<MgcMetaEdge> getNullifyingMetaEdge();

    void setMetaInfo(String startingStateId, Set<String> finishStateIds, O<MgcMetaEdge> nullifyingMetaEdge);
}
