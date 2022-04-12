package sk.outer.graph.nodes;

import sk.outer.graph.parser.MgcParsedData;
import sk.utils.statics.Cc;

public class MgcFictiveNode extends MgcNodeBase {

    public static final String FICTIVE_START = "_fictive_start";
    public static final String FICTIVE_END = "_fictive_end";

    public MgcFictiveNode(boolean start) {
        super(new MgcParsedData(start ? FICTIVE_START : FICTIVE_END, "$FICTIVE", Cc.l(), ""));
    }
}
