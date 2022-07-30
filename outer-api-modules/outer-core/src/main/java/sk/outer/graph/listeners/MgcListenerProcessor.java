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
import sk.outer.graph.parser.MgcTypeUtil;

import java.util.List;

public interface MgcListenerProcessor
        <CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>> {
    List<MgcListener<CTX, T, ?>> getListeners();

    <RES extends MgcListenerResult> void addListenerLast(MgcListener<CTX, T, RES> listener);

    <RES extends MgcListenerResult> void addAfter(MgcListener<CTX, T, RES> listener,
            Class<? extends MgcListener<CTX, T, RES>> cls);

    <RES extends MgcListenerResult> void addListenerFirst(MgcListener<CTX, T, RES> listener);

    MgcListenerResult getExceptionResult(Throwable e);

    default MgcListenerProcessorResult executeListeners(CTX context,
            MgcListenerProcessorResult listenerProcessor) {
        MgcListenerProcessorResult toRet = listenerProcessor;
        for (MgcListener<CTX, T, ?> listener : getListeners()) {
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
