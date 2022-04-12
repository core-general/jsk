package sk.outer.graph.parser;

import sk.outer.graph.edges.MgcEdge;
import sk.outer.graph.nodes.MgcNode;
import sk.utils.functional.O;

public interface MgcObjectGenerator {
    O<MgcEdge> getEdgeGenerator(MgcParsedData parsedData, boolean meta);

    O<MgcNode> getNodeGenerator(MgcParsedData parsedData);
}
