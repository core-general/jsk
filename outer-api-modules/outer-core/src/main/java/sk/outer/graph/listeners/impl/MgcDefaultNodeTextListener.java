package sk.outer.graph.listeners.impl;

import lombok.AllArgsConstructor;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.listeners.MgcListener;
import sk.outer.graph.listeners.MgcListenerResult;
import sk.outer.graph.nodes.MgcNode;

@AllArgsConstructor
public class MgcDefaultNodeTextListener implements MgcListener {
    public static final String id = "node_text";
    private MgcNode newNode;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public MgcListenerResult apply(MgcGraphExecutionContext context) {
        return new MgcNodeTextListenerResult(newNode.getText(newNode.getParsedData().getText(), context));
    }
}
