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
import sk.outer.graph.nodes.MgcGraph;
import sk.services.free.IFree;
import sk.utils.statics.Cc;

import java.util.List;


public class MgcModelUtils {
    public static String convertToGraphVizFormat(MgcGraph g, IFree client) {
        GvModel gvm = new GvModel(g.getAllEdgesFrom().stream()
                .map($ -> new GvEdge($.getId(), g.getEdgeSource($).getId(), g.getEdgeTarget($).getId()))
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
