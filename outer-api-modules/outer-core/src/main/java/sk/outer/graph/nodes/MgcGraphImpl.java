package sk.outer.graph.nodes;

import lombok.Getter;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedPseudograph;
import sk.outer.graph.edges.MgcEdge;
import sk.outer.graph.edges.MgcMetaEdge;
import sk.outer.graph.edges.MgcNormalEdge;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.execution.MgcGraphExecutionResult;
import sk.outer.graph.execution.MgcGraphExecutionResultImpl;
import sk.outer.graph.listeners.MgcListenerProcessorResultImpl;
import sk.outer.graph.parser.MgcParsedData;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;

import java.util.*;

public class MgcGraphImpl extends MgcNodeBase implements MgcGraph {
    protected Graph<MgcNode, MgcEdge> graph;
    @Getter protected String version;
    @Getter protected O<MgcGraph> parent;
    @Getter protected String startingStateId;
    @Getter protected Set<String> finishStateIds;
    @Getter protected O<MgcMetaEdge> nullifyingMetaEdge;

    volatile Map<MgcEdge, Integer> sortMap;
    List<MgcEdge> edges;
    protected MgcNodeBase fictiveStart;
    protected MgcNodeBase fictiveBack;

    {
        graph = new DirectedPseudograph<>(MgcEdge.class);
        graph.addVertex(fictiveStart = new MgcFictiveNode(true));
        graph.addVertex(fictiveBack = new MgcFictiveNode(false));
        edges = Cc.l();
    }

    public MgcGraphImpl(MgcParsedData pd, String version, O<MgcGraph> parent) {
        super(pd);
        this.version = version;
        this.parent = parent;
    }

    @Override
    public boolean addNormalEdge(MgcNode from, MgcNode to, MgcNormalEdge edge) {
        edges.add(edge);
        return graph.addEdge(from, to, edge);
    }

    @Override
    public boolean addMetaEdge(MgcNode to, MgcMetaEdge metaEdge) {
        edges.add(metaEdge);
        return graph.addEdge(fictiveStart, to, metaEdge);
    }

    @Override
    public boolean addMetaEdgeBack(MgcNode from, MgcMetaEdge metaEdge) {
        edges.add(metaEdge);
        return graph.addEdge(from, fictiveBack, metaEdge);
    }

    @Override
    public boolean addNode(MgcNode node) {
        return graph.addVertex(node);
    }

    @Override
    public List<MgcNode> getAllNodes() {
        return graph.vertexSet().stream().filter($ -> !($ instanceof MgcFictiveNode)).collect(Cc.toL());
    }

    @Override
    public O<MgcNode> getNodeById(String nodeId) {
        return O.of(graph.vertexSet().stream().filter($ -> Fu.equal($.getId(), nodeId)).findAny());
    }

    @Override
    public O<MgcEdge> getEdgeById(String edgeId) {
        return O.of(graph.edgeSet().stream().filter($ -> Fu.equal($.getId(), edgeId)).findAny());
    }

    @Override
    public MgcNode toNode(MgcEdge nextEdge, MgcGraphExecutionContext context) {
        if (Fu.equal(graph.getEdgeTarget(nextEdge).getId(), fictiveBack.getId()) && nextEdge instanceof MgcMetaEdge) {
            return ((MgcMetaEdge) nextEdge).getPossibleNodeIdIfMetaBack(context).flatMap($ -> getNodeById($))
                    .orElse(graph.getEdgeTarget(nextEdge));
        } else {
            return graph.getEdgeTarget(nextEdge);
        }
    }

    @Override
    public List<MgcNormalEdge> getDirectEdgesFrom(MgcNode node) {
        return graph.outgoingEdgesOf(node).stream()
                .filter($ -> $ instanceof MgcNormalEdge)
                .map($ -> (MgcNormalEdge) $)
                .sorted(compareEdges())
                .collect(Cc.toL());
    }

    @Override
    public List<MgcNormalEdge> getDirectEdgesTo(MgcNode node) {
        return graph.incomingEdgesOf(node).stream()
                .filter($ -> $ instanceof MgcNormalEdge)
                .map($ -> (MgcNormalEdge) $)
                .sorted(compareEdges())
                .collect(Cc.toL());
    }

    @Override
    public List<MgcMetaEdge> getMetaEdges() {
        return graph.outgoingEdgesOf(fictiveStart).stream()
                .filter($ -> $ instanceof MgcMetaEdge)
                .map($ -> (MgcMetaEdge) $)
                .sorted(compareEdges())
                .collect(Cc.toL());
    }

    @Override
    public List<MgcEdge> getMetaEdgesBack() {
        return graph.incomingEdgesOf(fictiveBack).stream()
                .filter($ -> $ instanceof MgcMetaEdge)
                .map($ -> (MgcMetaEdge) $)
                .sorted(compareEdges())
                .collect(Cc.toL());
    }

    @Override
    public List<MgcMetaEdge> getMetaEdgesBack(MgcNode node) {
        return graph.outgoingEdgesOf(node).stream()
                .filter($ -> $ instanceof MgcMetaEdge)
                .map($ -> (MgcMetaEdge) $)
                .sorted(compareEdges())
                .collect(Cc.toL());
    }

    @Override
    public MgcGraphExecutionResult instantiateResult(MgcListenerProcessorResultImpl edgeResult,
            MgcListenerProcessorResultImpl nodeResult,
            MgcGraphExecutionContext context) {
        return new MgcGraphExecutionResultImpl(context, false);
    }

    @Override
    public MgcNode getEdgeSource(MgcEdge $) {
        return graph.getEdgeSource($);
    }

    @Override
    public MgcNode getEdgeTarget(MgcEdge $) {
        return graph.getEdgeTarget($);
    }

    @Override
    public void setMetaInfo(String startingStateId, Set<String> finishStateIds, O<MgcMetaEdge> nullifyingMetaEdge) {
        this.startingStateId = startingStateId;
        this.finishStateIds = finishStateIds;
        this.nullifyingMetaEdge = nullifyingMetaEdge;
    }

    private Map<MgcEdge, Integer> getSortMap() {
        if (sortMap == null) {
            sortMap = new HashMap<>();
            Cc.eachWithIndex(edges, (e, i) -> sortMap.put(e, i));
        }
        return sortMap;
    }

    private Comparator<MgcEdge> compareEdges() {
        return (a, b) -> Fu.compare(getSortMap().get(a), getSortMap().get(b));
    }
}
