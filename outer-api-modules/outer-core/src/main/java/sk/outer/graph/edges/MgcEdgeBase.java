package sk.outer.graph.edges;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import sk.outer.graph.listeners.MgcListenerProcessorBase;
import sk.outer.graph.listeners.impl.MgcDefaultHistoryUpdaterListener;
import sk.outer.graph.parser.MgcParsedData;

@EqualsAndHashCode(of = {"parsedData"}, callSuper = false)
@Data
@AllArgsConstructor
public class MgcEdgeBase extends MgcListenerProcessorBase implements MgcEdge {
    MgcParsedData parsedData;

    {
        addListenerLast(MgcDefaultHistoryUpdaterListener.edge(this));
    }

    @Override
    public String getId() {
        return parsedData.getId();
    }
}
