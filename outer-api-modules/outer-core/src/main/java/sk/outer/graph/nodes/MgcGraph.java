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

import sk.outer.graph.edges.MgcEdge;
import sk.outer.graph.edges.MgcMetaEdge;
import sk.outer.graph.edges.MgcNormalEdge;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.parser.MgcTypeUtil;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface MgcGraph<CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>> {
    String getId();

    String getVersion();

    void addNormalEdge(MgcNode<CTX, T> from, MgcNode<CTX, T> to, MgcNormalEdge<CTX, T> edge);

    void addMetaEdge(MgcNode<CTX, T> to, MgcMetaEdge<CTX, T> metaEdge);

    void addMetaEdgeBack(MgcNode<CTX, T> from, MgcMetaEdge<CTX, T> metaEdge);

    void addNode(MgcNode<CTX, T> node);

    List<MgcNode<CTX, T>> getAllNodes();

    O<MgcNode<CTX, T>> getNodeById(String nodeId);

    O<MgcEdge<CTX, T>> getEdgeById(String edgeId);

    List<MgcNormalEdge<CTX, T>> getDirectEdgesFrom(MgcNode<CTX, T> node);

    List<MgcNormalEdge<CTX, T>> getDirectEdgesTo(MgcNode<CTX, T> node);

    List<MgcMetaEdge<CTX, T>> getAllMetaEdges();

    List<MgcMetaEdge<CTX, T>> getAllMetaEdgesBack();

    List<MgcMetaEdge<CTX, T>> getMetaEdgesBack(MgcNode<CTX, T> node);

    boolean isFictiveBack(MgcNode<CTX, T> node);

    default List<MgcEdge<CTX, T>> getEdgesFrom(MgcNode<CTX, T> node) {
        List<MgcEdge<CTX, T>> edges = Cc.l();
        edges.addAll(getDirectEdgesFrom(node));
        edges.addAll(getAllMetaEdges());
        edges.addAll(getMetaEdgesBack(node));
        return edges;
    }

    default List<MgcEdge<CTX, T>> getAllEdgesFrom() {
        return Cc.addAll(getAllDirectEdgesFrom(),
                new ArrayList<>(getAllMetaEdges()),
                new ArrayList<>(getAllMetaEdgesBack())
        );
    }

    default List<MgcEdge<CTX, T>> getAllDirectEdgesFrom() {
        return getAllNodes().stream().flatMap($ -> getDirectEdgesFrom($).stream()).distinct().collect(Cc.toL());
    }


    MgcNode<CTX, T> getEdgeSource(MgcEdge<CTX, T> $);

    MgcNode<CTX, T> getEdgeTarget(MgcEdge<CTX, T> $);

    String getStartingStateId();

    Set<String> getFinishStateIds();

    O<MgcMetaEdge<CTX, T>> getNullifyingMetaEdge();

    void setMetaInfo(String startingStateId, Set<String> finishStateIds, O<MgcMetaEdge<CTX, T>> nullifyingMetaEdge);
}
