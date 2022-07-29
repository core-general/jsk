package sk.outer.graph.nodes;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import lombok.Getter;
import org.jgrapht.Graph;
import org.jgrapht.graph.DirectedPseudograph;
import sk.outer.graph.edges.MgcEdge;
import sk.outer.graph.edges.MgcMetaEdge;
import sk.outer.graph.edges.MgcNormalEdge;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.parser.MgcTypeUtil;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;

import java.util.*;

public class MgcGraphImpl
        <CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>>
        implements MgcGraph<CTX, T> {
    protected final Graph<MgcNode<CTX, T>, MgcEdge<CTX, T>> graph;
    @Getter protected final String id;
    @Getter protected final String version;
    @Getter protected String startingStateId;
    @Getter protected Set<String> finishStateIds;
    @Getter
    protected O<MgcMetaEdge<CTX, T>> nullifyingMetaEdge;

    protected volatile Map<MgcEdge<CTX, T>, Integer> sortMap;
    protected final List<MgcEdge<CTX, T>> edges;
    protected MgcNodeBase<CTX, T> fictiveStart;
    protected MgcNodeBase<CTX, T> fictiveBack;

    public MgcGraphImpl(String id, String version, T fictiveNodeType) {
        this.id = id;
        this.version = version;

        //noinspection MoveFieldAssignmentToInitializer,unchecked,rawtypes
        graph = new DirectedPseudograph(MgcEdge.class);
        graph.addVertex(fictiveStart = new MgcFictiveNode<>(true, fictiveNodeType));
        graph.addVertex(fictiveBack = new MgcFictiveNode<>(false, fictiveNodeType));
        edges = Cc.l();
    }

    @Override
    public void addNormalEdge(MgcNode<CTX, T> from, MgcNode<CTX, T> to, MgcNormalEdge<CTX, T> edge) {
        edges.add(edge);
        graph.addEdge(from, to, edge);
    }

    @Override
    public void addMetaEdge(MgcNode<CTX, T> to, MgcMetaEdge<CTX, T> metaEdge) {
        edges.add(metaEdge);
        graph.addEdge(fictiveStart, to, metaEdge);
    }

    @Override
    public void addMetaEdgeBack(MgcNode<CTX, T> from, MgcMetaEdge<CTX, T> metaEdge) {
        edges.add(metaEdge);
        graph.addEdge(from, fictiveBack, metaEdge);
    }

    @Override
    public void addNode(MgcNode<CTX, T> node) {
        graph.addVertex(node);
    }

    @Override
    public List<MgcNode<CTX, T>> getAllNodes() {
        return graph.vertexSet().stream().filter($ -> !($ instanceof MgcFictiveNode)).collect(Cc.toL());
    }

    @Override
    public O<MgcNode<CTX, T>> getNodeById(String nodeId) {
        return O.of(graph.vertexSet().stream().filter($ -> Fu.equal($.getId(), nodeId)).findAny());
    }

    @Override
    public O<MgcEdge<CTX, T>> getEdgeById(String edgeId) {
        return O.of(graph.edgeSet().stream().filter($ -> Fu.equal($.getId(), edgeId)).findAny());
    }

    @Override
    public boolean isFictiveBack(MgcNode<CTX, T> node) {
        return Fu.equal(node.getId(), fictiveBack.getId());
    }

    @Override
    public List<MgcNormalEdge<CTX, T>> getDirectEdgesFrom(MgcNode<CTX, T> node) {
        return graph.outgoingEdgesOf(node).stream()
                .filter($ -> $ instanceof MgcNormalEdge)
                .map($ -> (MgcNormalEdge<CTX, T>) $)
                .sorted(compareEdges())
                .collect(Cc.toL());
    }

    @Override
    public List<MgcNormalEdge<CTX, T>> getDirectEdgesTo(MgcNode<CTX, T> node) {
        return graph.incomingEdgesOf(node).stream()
                .filter($ -> $ instanceof MgcNormalEdge)
                .map($ -> (MgcNormalEdge<CTX, T>) $)
                .sorted(compareEdges())
                .collect(Cc.toL());
    }

    @Override
    public List<MgcMetaEdge<CTX, T>> getAllMetaEdges() {
        return graph.outgoingEdgesOf(fictiveStart).stream()
                .filter($ -> $ instanceof MgcMetaEdge<CTX, T>)
                .map($ -> (MgcMetaEdge<CTX, T>) $)
                .sorted(compareEdges())
                .collect(Cc.toL());
    }

    @Override
    public List<MgcMetaEdge<CTX, T>> getAllMetaEdgesBack() {
        return graph.incomingEdgesOf(fictiveBack).stream()
                .filter($ -> $ instanceof MgcMetaEdge<CTX, T>)
                .map($ -> (MgcMetaEdge<CTX, T>) $)
                .sorted(compareEdges())
                .collect(Cc.toL());
    }

    @Override
    public List<MgcMetaEdge<CTX, T>> getMetaEdgesBack(MgcNode<CTX, T> node) {
        return graph.outgoingEdgesOf(node).stream()
                .filter($ -> $ instanceof MgcMetaEdge<CTX, T>)
                .map($ -> (MgcMetaEdge<CTX, T>) $)
                .sorted(compareEdges())
                .collect(Cc.toL());
    }


    @Override
    public MgcNode<CTX, T> getEdgeSource(MgcEdge<CTX, T> $) {
        return graph.getEdgeSource($);
    }

    @Override
    public MgcNode<CTX, T> getEdgeTarget(MgcEdge<CTX, T> $) {
        return graph.getEdgeTarget($);
    }

    @Override
    public void setMetaInfo(String startingStateId, Set<String> finishStateIds, O<MgcMetaEdge<CTX, T>> nullifyingMetaEdge) {
        this.startingStateId = startingStateId;
        this.finishStateIds = finishStateIds;
        this.nullifyingMetaEdge = nullifyingMetaEdge;
    }

    private Map<MgcEdge<CTX, T>, Integer> getSortMap() {
        if (sortMap == null) {
            sortMap = new HashMap<>();
            Cc.eachWithIndex(edges, (e, i) -> sortMap.put(e, i));
        }
        return sortMap;
    }

    private Comparator<MgcEdge<CTX, T>> compareEdges() {
        return (a, b) -> Fu.compare(getSortMap().get(a), getSortMap().get(b));
    }
}
