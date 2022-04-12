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
import sk.outer.graph.listeners.MgcListener;
import sk.outer.graph.listeners.MgcListenerResult;
import sk.outer.graph.nodes.MgcNode;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;

import static sk.outer.graph.listeners.impl.MgcDefaultNodeTextListener.id;

public class MgcDefaultHistoryUpdaterListener implements MgcListener {
    OneOf<MgcNode, MgcEdge> nodeOrEdge;

    private MgcDefaultHistoryUpdaterListener(OneOf<MgcNode, MgcEdge> nodeOrEdge) {
        this.nodeOrEdge = nodeOrEdge;
    }

    public static MgcDefaultHistoryUpdaterListener node(MgcNode nodeId) {
        return new MgcDefaultHistoryUpdaterListener(OneOf.left(nodeId));
    }

    public static MgcDefaultHistoryUpdaterListener edge(MgcEdge edgeId) {
        return new MgcDefaultHistoryUpdaterListener(OneOf.right(edgeId));
    }

    @Override
    public String getId() {
        return "history_updater";
    }

    @Override
    public MgcListenerResult apply(MgcGraphExecutionContext context) {
        MgcGraphHistoryItem item = new MgcGraphHistoryItem(context.getExecutedGraph().getId(),
                context.getExecutedGraph().getVersion(), nodeOrEdge.isLeft(),
                nodeOrEdge.collect($ -> $.getId(), $ -> $.getId()),
                nodeOrEdge.collect($ -> context.getNodeProcessor().getResultOf(id, MgcNodeTextListenerResult.class).getNewNode(),
                        $ -> context.getSelectedEdge()),
                nodeOrEdge.isLeft()
                ? context.getNodeProcessor()
                        .getResultOf(MgcDefaultEdgeVariantsListener.id, MgcEdgeVariantsListenerResult.class)
                        .getNormalEdges()
                : Cc.lEmpty(),
                nodeOrEdge.isLeft()
                ? context.getNodeProcessor()
                        .getResultOf(MgcDefaultEdgeVariantsListener.id, MgcEdgeVariantsListenerResult.class)
                        .getMetaEdges()
                : Cc.lEmpty());
        context.addGraphHistoryItem(item);
        return new MgcHistUpdateListenerResult(item);
    }
}
