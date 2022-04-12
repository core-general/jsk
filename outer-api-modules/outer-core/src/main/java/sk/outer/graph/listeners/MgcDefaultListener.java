package sk.outer.graph.listeners;

import lombok.AllArgsConstructor;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.utils.functional.F1;

@AllArgsConstructor
public class MgcDefaultListener implements MgcListener {
    String id;
    F1<MgcGraphExecutionContext, MgcListenerResult> processor;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public MgcListenerResult apply(MgcGraphExecutionContext mgcGraphExecutionContext) {
        return processor.apply(mgcGraphExecutionContext);
    }
}
