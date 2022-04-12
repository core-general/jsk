package sk.outer.graph.parser;

import sk.outer.graph.edges.MgcEdge;
import sk.utils.functional.O;

public interface MgcNodeGenerator extends MgcObjectGenerator {
    default O<MgcEdge> getEdgeGenerator(MgcParsedData parsedData, boolean meta) {
        return O.empty();
    }
}
