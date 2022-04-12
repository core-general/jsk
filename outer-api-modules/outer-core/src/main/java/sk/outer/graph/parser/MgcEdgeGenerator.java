package sk.outer.graph.parser;

import sk.outer.graph.nodes.MgcNode;
import sk.utils.functional.O;

public interface MgcEdgeGenerator extends MgcObjectGenerator {
    default O<MgcNode> getNodeGenerator(MgcParsedData parsedData) {
        return O.empty();
    }
}
