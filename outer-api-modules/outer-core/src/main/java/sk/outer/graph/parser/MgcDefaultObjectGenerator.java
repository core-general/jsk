package sk.outer.graph.parser;

import lombok.AllArgsConstructor;
import sk.outer.graph.edges.MgcEdge;
import sk.outer.graph.edges.MgcMetaEdge;
import sk.outer.graph.edges.MgcNormalEdge;
import sk.outer.graph.nodes.MgcNode;
import sk.outer.graph.nodes.MgcNodeBase;
import sk.utils.functional.F1;
import sk.utils.functional.O;

@AllArgsConstructor
public class MgcDefaultObjectGenerator implements MgcObjectGenerator {
    F1<MgcParsedData, MgcMetaEdge> metaEdge;
    F1<MgcParsedData, MgcNormalEdge> normalEdge;
    F1<MgcParsedData, MgcNode> node;

    public MgcDefaultObjectGenerator() {
        this(MgcMetaEdge::new, MgcNormalEdge::new, MgcNodeBase::new);
    }

    @Override
    public O<MgcEdge> getEdgeGenerator(MgcParsedData parsedData, boolean meta) {
        return O.of(meta ? metaEdge.apply(parsedData) : normalEdge.apply(parsedData));
    }

    @Override
    public O<MgcNode> getNodeGenerator(MgcParsedData parsedData) {
        return O.of(node.apply(parsedData));
    }
}
