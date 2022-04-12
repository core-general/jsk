package sk.outer.graph.listeners.impl;

import lombok.Data;
import sk.outer.graph.listeners.MgcBaseOkListenerResult;

@Data
public class MgcObjectListenerResult<T> extends MgcBaseOkListenerResult {
    final private T value;
}
