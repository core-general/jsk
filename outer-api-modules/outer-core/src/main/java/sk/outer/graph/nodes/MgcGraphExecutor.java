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

import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.outer.graph.edges.MgcEdge;
import sk.outer.graph.edges.MgcMetaEdge;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.execution.MgcGraphExecutionResult;
import sk.outer.graph.execution.MgcGraphExecutionResultImpl;
import sk.outer.graph.parser.MgcCtxProvider;
import sk.outer.graph.parser.MgcTypeUtil;
import sk.utils.functional.F2;
import sk.utils.functional.O;

import java.util.List;
import java.util.function.Supplier;

import static sk.utils.functional.O.*;

@AllArgsConstructor
public class MgcGraphExecutor
        <CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>> {
    @Getter protected MgcGraph<CTX, T> graph;

    public MgcGraphExecutionResult<CTX, T> executeByHistory(O<String> selectedEdge,
            MgcCtxProvider<CTX, T> contextProvider) {
        final CTX ctx = contextProvider.getContext(this, empty(), selectedEdge);
        return !ctx.getHistory().hasHistory()
               ? executeFirst(contextProvider)
               : executeAnyNode(ctx.getFromNode().get(), selectedEdge.get(), contextProvider);
    }

    public MgcGraphExecutionResult<CTX, T> executeAnyNode(MgcNode<CTX, T> currentNode, String selectedEdge,
            MgcCtxProvider<CTX, T> contextProvider) {
        final CTX context = contextProvider.getContext(this, of(currentNode), of(selectedEdge));
        return searchNextEdge(currentNode, selectedEdge, context)
                .map($ -> privateExecute($, context))
                .orElseGet(() -> new MgcGraphExecutionResultImpl<>(context, true));
    }

    protected MgcGraphExecutionResult<CTX, T> executeFirst(MgcCtxProvider<CTX, T> contextProvider) {
        final MgcNode<CTX, T> initial = graph.getNodeById(graph.getStartingStateId()).get();
        return privateFirstExecute(initial, contextProvider.getContext(this, of(initial), empty()));
    }

    protected O<MgcEdge<CTX, T>> searchNextEdge(MgcNode<CTX, T> currentNode, String selectedEdge, CTX context) {
        F2<List<? extends MgcEdge<CTX, T>>, Supplier<MgcEdge<CTX, T>>, MgcEdge<CTX, T>> processor =
                (mgcEdges, supplier) -> mgcEdges.stream()
                        .filter($ -> $.acceptEdge(selectedEdge, context))
                        .map($ -> (MgcEdge<CTX, T>) $).findFirst()
                        .orElseGet(supplier);

        MgcEdge<CTX, T> edge = processor.apply(getGraph().getAllMetaEdges(),
                () -> processor.apply(getGraph().getMetaEdgesBack(currentNode),
                        () -> processor.apply(getGraph().getDirectEdgesFrom(currentNode), () -> null)));


        return ofNullable(edge);
    }

    protected MgcGraphExecutionResult<CTX, T> privateExecute(MgcEdge<CTX, T> nextEdge, CTX context) {
        MgcNode<CTX, T> to = toNode(nextEdge, context);
        context.getToNodeHolder().set(to);
        nextEdge.executeListeners(context, context.getResults().getEdgeListeners());
        to.executeListeners(context, context.getResults().getNodeListeners());
        return new MgcGraphExecutionResultImpl<>(context, false);
    }

    protected MgcGraphExecutionResult<CTX, T> privateFirstExecute(MgcNode<CTX, T> initial, CTX context) {
        context.getToNodeHolder().set(initial);
        initial.executeListeners(context, context.getResults().getNodeListeners());
        return new MgcGraphExecutionResultImpl<>(context, false);
    }

    protected MgcNode<CTX, T> toNode(MgcEdge<CTX, T> nextEdge, CTX context) {
        if (graph.isFictiveBack(graph.getEdgeTarget(nextEdge)) && nextEdge instanceof MgcMetaEdge) {
            return ((MgcMetaEdge<CTX, T>) nextEdge)
                    .getPossibleNodeIdIfMetaBack(context).flatMap($ -> getGraph().getNodeById($))
                    .orElse(graph.getEdgeTarget(nextEdge));
        } else {
            return graph.getEdgeTarget(nextEdge);
        }
    }
}
