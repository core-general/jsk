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

import lombok.AllArgsConstructor;
import sk.outer.graph.edges.MgcMetaEdge;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.listeners.MgcListener;
import sk.outer.graph.listeners.MgcListenerResult;
import sk.outer.graph.nodes.MgcNode;
import sk.outer.graph.parser.MgcTypeUtil;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class MgcDefaultEdgeVariantsListener
        <CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>>
        implements MgcListener<CTX, T> {
    public static final String id = "edge_possibilities";
    private MgcNode<CTX, T> newNode;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public MgcListenerResult apply(CTX context) {
        List<String> allPossibleEdges = context.getExecutedGraph().getGraph().getEdgesFrom(newNode).stream()
                .filter($ -> !($ instanceof MgcMetaEdge))
                .flatMap($ -> $.getPossibleEdges($.getParsedData().getText(), context).stream())
                .collect(Collectors.toList());
        List<String> metaEdges = context.getExecutedGraph().getGraph().getEdgesFrom(newNode).stream()
                .filter($ -> $ instanceof MgcMetaEdge)
                .flatMap($ -> $.getPossibleEdges($.getParsedData().getText(), context).stream())
                .collect(Collectors.toList());
        return new MgcEdgeVariantsListenerResult(allPossibleEdges, metaEdges);
    }
}
