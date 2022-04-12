package sk.outer.graph.edges;


import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.parser.MgcParsedData;
import sk.utils.statics.Cc;

import java.util.List;
import java.util.function.BiPredicate;

public class MgcAnyTextEdge extends MgcNormalEdge {
    public MgcAnyTextEdge(MgcParsedData parsedData) {super(parsedData);}

    @Override
    public BiPredicate<String, String> getAcceptPredicate() {
        return (_1, _2) -> true;
    }

    @Override
    public List<String> getPossibleEdges(String template, MgcGraphExecutionContext context) {
        return Cc.l();
    }
}

