package sk.outer.graph.listeners;

import lombok.Data;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class MgcListenerProcessorBase implements MgcListenerProcessor {
    List<MgcListener> listeners = new ArrayList<>();
    Set<String> listenerSet = new HashSet<>();

    @Override
    public void addListenerLast(MgcListener listener) {
        if (listenerSet.contains(listener.getId())) {
            throw new RuntimeException("Listeners with same id:" + listener.getId());
        }
        listeners.add(listener);
        listenerSet.add(listener.getId());
    }

    @Override
    public void addAfter(MgcListener listener, Class<? extends MgcListener> cls) {
        if (listenerSet.contains(listener.getId())) {
            throw new RuntimeException("Listeners with same id:" + listener.getId());
        }
        int index = Cc.firstIndex(listeners, $ -> Fu.equal($.getClass(), cls));
        if (index > 0) {
            if (index == listeners.size() - 1) {
                listeners.add(listener);
            } else {
                listeners.add(index + 1, listener);
            }
            listenerSet.add(listener.getId());
        }
    }

    @Override
    public void addListenerFirst(MgcListener listener) {
        if (listenerSet.contains(listener.getId())) {
            throw new RuntimeException("Listeners with same id:" + listener.getId());
        }
        listeners.add(0, listener);
        listenerSet.add(listener.getId());
    }

    @Override
    public MgcListenerResult getExceptionResult(Throwable e) {
        return new MgcBaseListenerResult(true, true, O.of(e));
    }
}
