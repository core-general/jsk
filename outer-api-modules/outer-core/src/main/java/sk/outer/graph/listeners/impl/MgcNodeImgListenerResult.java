package sk.outer.graph.listeners.impl;

import lombok.Data;
import sk.outer.graph.listeners.MgcBaseOkListenerResult;
import sk.utils.functional.O;

@Data
public class MgcNodeImgListenerResult extends MgcBaseOkListenerResult {
    final O<String> newNode;
}
