package sk.outer.graph.edges;

import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.parser.MgcParsedData;
import sk.utils.functional.O;

public class MgcMetaEdge extends MgcEdgeBase {
    public MgcMetaEdge(MgcParsedData parsedData) {
        super(parsedData);
    }

    public O<String> getPossibleNodeIdIfMetaBack(MgcGraphExecutionContext context) {
        return O.empty();
    }
}
