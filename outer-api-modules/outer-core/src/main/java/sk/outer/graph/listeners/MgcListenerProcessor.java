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
