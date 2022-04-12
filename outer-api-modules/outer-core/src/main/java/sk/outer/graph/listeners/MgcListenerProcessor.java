package sk.outer.graph.listeners;

import sk.outer.graph.execution.MgcGraphExecutionContext;

import java.util.List;

public interface MgcListenerProcessor {
    void addListenerLast(MgcListener listener);

    List<MgcListener> getListeners();

    void addAfter(MgcListener listener, Class<? extends MgcListener> cls);

    void addListenerFirst(MgcListener listener);

    MgcListenerResult getExceptionResult(Throwable e);

    default MgcListenerProcessorResultImpl executeListeners(MgcGraphExecutionContext context,
            MgcListenerProcessorResultImpl listenerProcessor) {
        MgcListenerProcessorResultImpl toRet = listenerProcessor;
        for (MgcListener listener : getListeners()) {
            MgcListenerResult apply = null;
            try {
                apply = listener.apply(context);
            } catch (Throwable e) {
                apply = getExceptionResult(e);
            }
            toRet.addListenerResult(listener.getId(), apply);
            if (apply.isError() && apply.isStopper()) {
                break;
            }
        }
        return toRet;
    }
}
