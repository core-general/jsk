package sk.outer.graph.listeners;

import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.Map;

public class MgcListenerProcessorResultImpl {
    Map<String, MgcListenerResult> results = Cc.m();

    public void addListenerResult(String listenerId, MgcListenerResult result) {
        results.put(listenerId, result);
    }

    public boolean isError() {
        return results.values().stream().filter($ -> $.isError() && $.isStopper()).count() > 0;
    }

    public O<Throwable> getError() {
        return O.of(results.values().stream().filter($ -> $.isError() && $.isStopper()).findAny()
                .flatMap($ -> $.getException().toOpt()));
    }

    public <T extends MgcListenerResult> T getResultOf(String s, Class<T> cls) {
        return (T) results.get(s);
    }
}
