package sk.outer.graph.parser;

import sk.outer.graph.nodes.MgcGraph;
import sk.utils.functional.O;

public interface MgcParseEnv {
    O<MgcObjectGenerator> getGenerator(O<String> type);

    default O<MgcGraph> getOrCreateInnerGraph(String graphId) {
        return O.empty();
    }

    public default int maxSizeOfEdgeText() {
        return 70;
    }

    default boolean isEdgeSizeOk(MgcParsedData mgcParsedData) {
        return false;
    }
}
