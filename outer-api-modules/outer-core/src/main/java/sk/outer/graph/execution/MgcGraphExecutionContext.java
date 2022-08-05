package sk.outer.graph.execution;

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
import sk.outer.graph.MgcHistoryProvider;
import sk.outer.graph.listeners.impl.MgcObjectListenerResult;
import sk.outer.graph.nodes.MgcGraphExecutor;
import sk.outer.graph.nodes.MgcNode;
import sk.outer.graph.parser.MgcTypeUtil;
import sk.utils.functional.GSet;
import sk.utils.functional.O;
import sk.utils.tuples.X1;

@Getter
public abstract class MgcGraphExecutionContext<CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>> {
    final protected MgcGraphExecutor<CTX, T> executedGraph;
    private O<MgcNode<CTX, T>> fromNode;
    final private O<MgcNode<CTX, T>> fromNodeInitial;
    final protected O<String> selectedEdge;
    final protected int nestingLevel;

    final protected GSet<MgcNode<CTX, T>> toNodeHolder = new X1<>();
    final protected MgcListenerResults results = new MgcListenerResults();
    protected MgcHistoryProvider history;

    public abstract MgcHistoryProvider initHistoryProvider();

    public MgcGraphExecutionContext(MgcGraphExecutor<CTX, T> executedGraph, O<MgcNode<CTX, T>> fromNode, O<String> selectedEdge,
            int nestingLevel) {
        this.executedGraph = executedGraph;
        fromNodeInitial = fromNode;
        this.selectedEdge = selectedEdge;
        this.nestingLevel = nestingLevel;
    }

    public O<MgcNode<CTX, T>> getFromNode() {
        if (fromNode == null) {
            if (fromNodeInitial.isEmpty() && selectedEdge.isPresent()) {
                //find in history
                this.fromNode = getHistory().getLastNode()
                        .flatMap(lstNode -> {
                            if (lstNode.getNestingLevel() == getNestingLevel()) {
                                return executedGraph.getGraph().getNodeById(lstNode.getId());
                            } else {
                                return lstNode.getNestedGraphInfo().get(nestingLevel + 1)
                                        .getPreviousLevelNestedGraphNodeId()
                                        .flatMap($$ -> executedGraph.getGraph().getNodeById($$));
                            }
                        })
                ;
            } else {
                this.fromNode = fromNodeInitial;
            }
        }
        return fromNode;
    }

    public <T> T getObjectResultOf(String listenerId, Class<T> cls) {
        final MgcObjectListenerResult result =
                results.getNodeListeners().getResultOf(listenerId, MgcObjectListenerResult.class)
                        .or(() -> results.getEdgeListeners().getResultOf(listenerId, MgcObjectListenerResult.class)).get();

        return (T) result.getValue();
    }

    public MgcHistoryProvider getHistory() {
        return history == null ? history = initHistoryProvider() : history;
    }
}
