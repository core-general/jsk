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
public sealed class MgcGraphExecutor
        <CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>>
        permits MgcGraphExecutorDecorator {
    public static final int STARTING_NESTING_LEVEL = 0;
    @Getter protected MgcGraph<CTX, T> graph;

    public O<MgcGraphExecutionResult> executeNullifyingMetaEdge(MgcCtxProvider<CTX, T> contextProvider) {
        return graph.getNullifyingMetaEdge().map(nullifyMetaedge -> {
            final MgcNode<CTX, T> target = graph.getEdgeTarget(nullifyMetaedge);
            return privateExecute(nullifyMetaedge, contextProvider.getContext(this, empty(), empty(), 0), of(target));
        });
    }

    public MgcGraphExecutionResult executeByHistory(O<String> selectedEdge,
            MgcCtxProvider<CTX, T> contextProvider) {
        return executeByHistoryNested(selectedEdge, contextProvider, STARTING_NESTING_LEVEL);
    }

    public MgcGraphExecutionResult executeByHistoryNested(O<String> selectedEdge,
            MgcCtxProvider<CTX, T> contextProvider, int nestingLevel) {
        final CTX ctx = contextProvider.getContext(this, empty(), selectedEdge, nestingLevel);
        return ctx.getHistory().isFirstTimeOnThisNestingLevelWhenGoingDown(nestingLevel)
               ? executeFirst(contextProvider, nestingLevel)
               : executeAnyNodeNested(ctx.getFromNode().get(), selectedEdge.get(), nestingLevel, contextProvider);
    }

    public MgcGraphExecutionResult executeAnyNode(MgcNode<CTX, T> currentNode, String selectedEdge,
            MgcCtxProvider<CTX, T> contextProvider) {
        return executeAnyNodeNested(currentNode, selectedEdge, STARTING_NESTING_LEVEL, contextProvider);
    }

    public MgcGraphExecutionResult executeAnyNodeNested(MgcNode<CTX, T> currentNode, String selectedEdge, int nestingLevel,
            MgcCtxProvider<CTX, T> contextProvider) {
        final CTX context = contextProvider.getContext(this, of(currentNode), of(selectedEdge), nestingLevel);

        O<MgcGraphExecutionResult> nestedGraphExecution =
                (currentNode instanceof MgcNestedGraphNode<CTX, T, ?, ?> nestedGraph)
                ? executeNestedGraph(context, nestedGraph, selectedEdge)
                : empty();

        return nestedGraphExecution.orElseGet(() -> {
            return searchNextEdge(currentNode, selectedEdge, context)
                    .map(edge -> {
                        final MgcNode<CTX, T> nextNode = findNextNode(edge, context);
                        if (nextNode instanceof MgcNestedGraphNode<CTX, T, ?, ?> nested) {
                            return executedNestedFirst(of(edge), nested, context);
                        } else {
                            return privateExecute(edge, context, of(nextNode));
                        }
                    })
                    .orElseGet(() -> new MgcGraphExecutionResultImpl(context.getResults(), true,
                            false, isReachedFinalNode(context, currentNode)));
        });
    }


    private MgcGraphExecutionResult executeFirst(MgcCtxProvider<CTX, T> contextProvider, int nestingLevel) {
        final MgcNode<CTX, T> initial = graph.getNodeById(graph.getStartingStateId()).get();
        final CTX context = contextProvider.getContext(this, of(initial), empty(), nestingLevel);

        if (initial instanceof MgcNestedGraphNode<CTX, T, ?, ?> nested) {
            return executedNestedFirst(empty(), nested, context);
        } else {
            return privateFirstExecute(initial, context);
        }
    }


    private MgcGraphExecutionResult privateExecute(MgcEdge<CTX, T> nextEdge, CTX context, O<MgcNode<CTX, T>> alreadyFoundNode) {
        MgcNode<CTX, T> to = alreadyFoundNode.orElseGet(() -> findNextNode(nextEdge, context));
        context.getToNodeHolder().set(to);
        nextEdge.executeListeners(context, context.getResults().getEdgeListeners());
        to.executeListeners(context, context.getResults().getNodeListeners());

        flushHistoryIfNeeded(context);

        return new MgcGraphExecutionResultImpl(context.getResults(), false, false, isReachedFinalNode(context, to));
    }

    private MgcGraphExecutionResult privateFirstExecute(MgcNode<CTX, T> initial, CTX context) {
        context.getToNodeHolder().set(initial);
        initial.executeListeners(context, context.getResults().getNodeListeners());

        flushHistoryIfNeeded(context);

        return new MgcGraphExecutionResultImpl(context.getResults(), false, true, isReachedFinalNode(context, initial));
    }

    private O<MgcGraphExecutionResult> executeNestedGraph(CTX ctx, MgcNestedGraphNode<CTX, T, ?, ?> ngn, String selectedEdge) {
        return ngn.executeNestedGraph(ctx, selectedEdge);
    }

    private MgcGraphExecutionResult executedNestedFirst(O<MgcEdge<CTX, T>> edge, MgcNestedGraphNode<CTX, T, ?, ?> ngn, CTX ctx) {
        return ngn.executedNestedFirst(edge, ctx);
    }

    private boolean isReachedFinalNode(CTX context, MgcNode<CTX, T> to) {
        return context.getExecutedGraph().getGraph().getNormalEdgesFrom(to).size() == 0;
    }

    private void flushHistoryIfNeeded(CTX context) {
        if (context.getNestingLevel() == STARTING_NESTING_LEVEL) {context.getHistory().flush();}
    }

    private O<MgcEdge<CTX, T>> searchNextEdge(MgcNode<CTX, T> currentNode, String selectedEdge, CTX context) {
        F2<List<? extends MgcEdge<CTX, T>>, Supplier<MgcEdge<CTX, T>>, MgcEdge<CTX, T>> processor =
                (mgcEdges, supplier) -> mgcEdges.stream()
                        .filter($ -> $.acceptEdge(selectedEdge, context))
                        .map($ -> (MgcEdge<CTX, T>) $).findFirst()
                        .orElseGet(supplier);

        MgcEdge<CTX, T> edge = processor.apply(getGraph().getAllMetaEdges(),
                () -> processor.apply(getGraph().getMetaEdgesBack(currentNode),
                        () -> processor.apply(getGraph().getNormalEdgesFrom(currentNode), () -> null)));


        return ofNullable(edge);
    }

    private MgcNode<CTX, T> findNextNode(MgcEdge<CTX, T> nextEdge, CTX context) {
        if (graph.isFictiveBack(graph.getEdgeTarget(nextEdge)) && nextEdge instanceof MgcMetaEdge) {
            return ((MgcMetaEdge<CTX, T>) nextEdge)
                    .getPossibleNodeIdIfMetaBack(context).flatMap($ -> getGraph().getNodeById($))
                    .orElse(graph.getEdgeTarget(nextEdge));
        } else {
            return graph.getEdgeTarget(nextEdge);
        }
    }
}
