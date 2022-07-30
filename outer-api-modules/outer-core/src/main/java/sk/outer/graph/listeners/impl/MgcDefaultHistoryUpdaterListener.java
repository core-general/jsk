package sk.outer.graph.listeners.impl;

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
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.execution.MgcGraphHistoryItem;
import sk.outer.graph.execution.MgcGraphInfo;
import sk.outer.graph.listeners.MgcDefaultListener;
import sk.outer.graph.nodes.MgcNode;
import sk.outer.graph.parser.MgcTypeUtil;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;

import static sk.outer.graph.listeners.impl.MgcDefaultNodeTextListener.id;

public class MgcDefaultHistoryUpdaterListener
        <CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>>
        extends MgcDefaultListener<CTX, T, MgcHistUpdateListenerResult> {
    OneOf<MgcNode<CTX, T>, MgcEdge<CTX, T>> nodeOrEdge;

    private MgcDefaultHistoryUpdaterListener(OneOf<MgcNode<CTX, T>, MgcEdge<CTX, T>> nodeOrEdge) {
        super("history_updater", context -> {
            final O<MgcEdgeVariantsListenerResult> edgeResult = context.getResults().getNodeListeners()
                    .getResultOf(MgcDefaultEdgeVariantsListener.id, MgcEdgeVariantsListenerResult.class);

            O<MgcGraphHistoryItem> lastNode = context.getHistory().getLastNode();

            MgcGraphHistoryItem item = MgcGraphHistoryItem.newItem(
                    context.getNestingLevel(),
                    lastNode,
                    new MgcGraphInfo(
                            context.getExecutedGraph().getGraph().getId(),
                            context.getExecutedGraph().getGraph().getVersion()),
                    nodeOrEdge.collect(__ -> true, __ -> false),
                    nodeOrEdge.collect($ -> $.getId(), $ -> $.getId()),
                    nodeOrEdge.collect(
                            __ -> context.getResults().getNodeListeners()
                                    .getResultOf(id, MgcNodeTextListenerResult.class).map($ -> $.getNewNodeText()).orElse(""),
                            __ -> context.getSelectedEdge().orElse("")),
                    nodeOrEdge.collect(
                            __ -> edgeResult.map($ -> $.getNormalEdges()).orElse(Cc.lEmpty()),
                            __ -> Cc.lEmpty()),
                    nodeOrEdge.collect(
                            __ -> edgeResult.map($ -> $.getMetaEdges()).orElse(Cc.lEmpty()),
                            __ -> Cc.lEmpty())
            );
            context.getHistory().addGraphHistoryItem(item);
            return new MgcHistUpdateListenerResult(item);
        });
        this.nodeOrEdge = nodeOrEdge;
    }

    public static <CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>>
    MgcDefaultHistoryUpdaterListener<CTX, T> node(
            MgcNode<CTX, T> nodeId) {
        return new MgcDefaultHistoryUpdaterListener<>(OneOf.left(nodeId));
    }

    public static <CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>>
    MgcDefaultHistoryUpdaterListener<CTX, T> edge(MgcEdge<CTX, T> edgeId) {
        return new MgcDefaultHistoryUpdaterListener<>(OneOf.right(edgeId));
    }
}
