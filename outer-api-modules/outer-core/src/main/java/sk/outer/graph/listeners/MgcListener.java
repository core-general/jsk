package sk.outer.graph.listeners;

import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.utils.functional.F1;
import sk.utils.ifaces.IdentifiableString;

public interface MgcListener extends F1<MgcGraphExecutionContext, MgcListenerResult>, IdentifiableString {
}
