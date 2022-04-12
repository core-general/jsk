package sk.outer.graph.execution;

import sk.utils.functional.O;

public interface MgcGraphExecutionResult {
    default boolean isError() {
        return isCantFindEdge()
                || getContext().getEdgeProcessor().isError()
                || getContext().getNodeProcessor().isError();
    }

    default O<Throwable> getError() {
        if (isCantFindEdge()) {
            return O.of(new RuntimeException("Can't find edge"));
        } else if (getContext().getEdgeProcessor().isError()) {
            return getContext().getEdgeProcessor().getError();
        } else if (getContext().getNodeProcessor().isError()) {
            return getContext().getNodeProcessor().getError();
        }
        return O.empty();
    }

    boolean isCantFindEdge();

    MgcGraphExecutionContext getContext();
}
