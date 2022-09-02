package sk.outer.graph;

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
import lombok.Data;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.nodes.MgcGraph;
import sk.outer.graph.nodes.MgcNode;
import sk.outer.graph.parser.MgcTypeUtil;
import sk.services.free.IFree;
import sk.utils.statics.Cc;
import sk.utils.statics.St;

import java.util.List;


public class MgcModelUtils {

    public static final int LIMIT = 20;

    public static <CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>>
    String convertToGraphVizFormat(MgcGraph<CTX, T> g, IFree client) {
        GvModel gvm = new GvModel(g.getAllEdgesFrom().stream()
                .map($ -> {
                    final MgcNode<CTX, T> edgeSource = g.getEdgeSource($);
                    final MgcNode<CTX, T> targetNode = g.getEdgeTarget($);
                    String edgeTxt = edgeSource.getId() + "\\n" + St.raze3dots(edgeSource.getParsedData().getText(), LIMIT);
                    String nodeTxt = targetNode.getId() + "\\n" + St.raze3dots(targetNode.getParsedData().getText(), LIMIT);
                    String curText = $.getId() + "\\n" + St.raze3dots($.getParsedData().getText(), LIMIT);
                    return new GvEdge(curText, edgeTxt, nodeTxt);
                })
                .collect(Cc.toL()));

        return client.process("mgc_graph/graphviz_templates/digraph.ftl", Cc.m("model", gvm));
    }


    @Data
    @AllArgsConstructor
    public static class GvModel {
        List<GvEdge> edges;
    }

    @Data
    @AllArgsConstructor
    public static class GvEdge {
        String id;
        String left;
        String right;
    }
}
