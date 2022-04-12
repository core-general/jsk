package sk.outer.graph.nodes;

import sk.outer.graph.MgcParsedDataHolder;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.listeners.MgcListenerProcessor;
import sk.utils.functional.O;
import sk.utils.ifaces.IdentifiableString;

public interface MgcNode extends MgcListenerProcessor, IdentifiableString, MgcParsedDataHolder {
    default String getText(String template, MgcGraphExecutionContext context) {
        return template;
    }

    default O<String> getImage(String template, MgcGraphExecutionContext context) {
        return O.empty();
    }
}
