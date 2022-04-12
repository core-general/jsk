package sk.outer.graph.nodes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import sk.outer.graph.listeners.MgcListenerProcessorBase;
import sk.outer.graph.listeners.impl.MgcDefaultEdgeVariantsListener;
import sk.outer.graph.listeners.impl.MgcDefaultHistoryUpdaterListener;
import sk.outer.graph.listeners.impl.MgcDefaultNodeImgListener;
import sk.outer.graph.listeners.impl.MgcDefaultNodeTextListener;
import sk.outer.graph.parser.MgcParsedData;

@EqualsAndHashCode(of = {"parsedData"}, callSuper = false)
@Data
@AllArgsConstructor
public class MgcNodeBase extends MgcListenerProcessorBase implements MgcNode {
    MgcParsedData parsedData;

    {
        addListenerLast(new MgcDefaultEdgeVariantsListener(this));
        addListenerLast(new MgcDefaultNodeTextListener(this));
        addListenerLast(new MgcDefaultNodeImgListener(this));
        addListenerLast(MgcDefaultHistoryUpdaterListener.node(this));
    }

    @Override
    public String getId() {
        return parsedData.getId();
    }
}
