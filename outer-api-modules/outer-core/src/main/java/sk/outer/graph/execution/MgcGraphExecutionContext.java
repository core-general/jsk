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
import sk.outer.graph.nodes.MgcGraphExecutor;
import sk.outer.graph.nodes.MgcNode;
import sk.outer.graph.parser.MgcTypeUtil;
import sk.utils.functional.GSet;
import sk.utils.functional.O;
import sk.utils.tuples.X1;

@Getter
public abstract class MgcGraphExecutionContext<CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>> {
    final protected MgcGraphExecutor<CTX, T> executedGraph;
    final protected O<MgcNode<CTX, T>> fromNode;
    final protected O<String> selectedEdge;

    final protected GSet<MgcNode<CTX, T>> toNodeHolder = new X1<>();
    final protected MgcListenerResults results = new MgcListenerResults();
    final protected MgcHistoryProvider history = initHistoryProvider();

    public abstract MgcHistoryProvider initHistoryProvider();

    public MgcGraphExecutionContext(MgcGraphExecutor<CTX, T> executedGraph, O<MgcNode<CTX, T>> fromNode, O<String> selectedEdge) {
        this.executedGraph = executedGraph;
        if (fromNode.isEmpty() && selectedEdge.isPresent()) {
            //find in history
            this.fromNode = history.getLastNode().map($ -> executedGraph.getGraph().getNodeById($.getId()).get());
        } else {
            this.fromNode = fromNode;
        }
        this.selectedEdge = selectedEdge;
    }
}
