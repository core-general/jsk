package sk.outer.graph.listeners;

import sk.utils.functional.O;

public interface MgcListenerResult {
    boolean isError();

    boolean isStopper();

    O<Throwable> getException();
}
