package sk.outer.graph.listeners;

import lombok.AllArgsConstructor;
import lombok.Data;
import sk.utils.functional.O;

@AllArgsConstructor
@Data
public class MgcBaseListenerResult implements MgcListenerResult {
    boolean error;
    boolean stopper;
    O<Throwable> exception;
}
