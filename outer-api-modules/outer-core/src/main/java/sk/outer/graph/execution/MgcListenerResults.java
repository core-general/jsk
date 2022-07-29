package sk.outer.graph.execution;

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

import lombok.Getter;
import sk.outer.graph.listeners.MgcListenerProcessorResult;
import sk.outer.graph.listeners.impl.MgcDefaultEdgeVariantsListener;
import sk.outer.graph.listeners.impl.MgcDefaultNodeTextListener;
import sk.outer.graph.listeners.impl.MgcEdgeVariantsListenerResult;
import sk.outer.graph.listeners.impl.MgcNodeTextListenerResult;
import sk.utils.functional.O;

public class MgcListenerResults {
    @Getter MgcListenerProcessorResult nodeListeners = new MgcListenerProcessorResult();
    @Getter MgcListenerProcessorResult edgeListeners = new MgcListenerProcessorResult();

    public MgcEdgeVariantsListenerResult getPossibleEdges() {
        return nodeListeners.getResultOf(MgcDefaultEdgeVariantsListener.id, MgcEdgeVariantsListenerResult.class);
    }

    public MgcNodeTextListenerResult getNewNodeInfo() {
        return nodeListeners.getResultOf(MgcDefaultNodeTextListener.id, MgcNodeTextListenerResult.class);
    }

    public boolean isError() {
        return getEdgeListeners().isError() || getNodeListeners().isError();
    }

    public O<Throwable> getError() {
        return getEdgeListeners().getError().or(() -> getNodeListeners().getError());
    }
}
