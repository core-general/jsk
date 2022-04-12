package sk.outer.graph.listeners.impl;

import lombok.Data;
import sk.outer.graph.listeners.MgcBaseOkListenerResult;

@Data
public class MgcNodeTextListenerResult extends MgcBaseOkListenerResult {
    final String newNode;
}
