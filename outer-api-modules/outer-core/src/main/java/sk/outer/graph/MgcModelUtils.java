package sk.outer.graph;

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
