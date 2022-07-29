package sk.outer.graph.listeners;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import lombok.Data;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.parser.MgcTypeUtil;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class MgcListenerProcessorBase
        <CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>>
        implements MgcListenerProcessor<CTX, T> {

    List<MgcListener<CTX, T>> listeners = new ArrayList<>();
    Set<String> listenerSet = new HashSet<>();

    @Override
    public void addListenerLast(MgcListener<CTX, T> listener) {
        if (listenerSet.contains(listener.getId())) {
            throw new RuntimeException("Listeners with same id:" + listener.getId());
        }
        listeners.add(listener);
        listenerSet.add(listener.getId());
    }

    @Override
    public void addAfter(MgcListener<CTX, T> listener, Class<? extends MgcListener<CTX, T>> cls) {
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
    public void addListenerFirst(MgcListener<CTX, T> listener) {
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
