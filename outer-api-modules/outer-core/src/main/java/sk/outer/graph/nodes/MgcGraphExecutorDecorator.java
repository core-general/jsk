package sk.outer.graph.nodes;

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
import sk.outer.graph.execution.MgcGraphExecutionResult;
import sk.outer.graph.parser.MgcCtxProvider;
import sk.outer.graph.parser.MgcTypeUtil;
import sk.utils.functional.O;

public final class MgcGraphExecutorDecorator<CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>>
        extends MgcGraphExecutor<CTX, T> {

    private MgcGraphExecutor<CTX, T> executor;
    private volatile MgcGraphExecutor<CTX, T> executorMulti;
    private final boolean multiThreadedMode;

    public MgcGraphExecutorDecorator(MgcGraphExecutor<CTX, T> executor, boolean multiThreadedMode) {
        super(null);
        this.multiThreadedMode = multiThreadedMode;
        setExecutor(executor);
    }

    public void setExecutor(MgcGraphExecutor<CTX, T> executor) {
        if (multiThreadedMode) {
            this.executorMulti = executor;
        } else {
            this.executor = executor;
        }
    }

    public MgcGraphExecutor<CTX, T> getExecutor() {
        return multiThreadedMode ? executorMulti : executor;
    }

    @Override
    public MgcGraph<CTX, T> getGraph() {
        return getExecutor().getGraph();
    }

    @Override
    public MgcGraphExecutionResult executeByHistory(O<String> selectedEdge, MgcCtxProvider<CTX, T> contextProvider) {
        return getExecutor().executeByHistory(selectedEdge, contextProvider);
    }

    @Override
    public MgcGraphExecutionResult executeByHistoryNested(O<String> selectedEdge, MgcCtxProvider<CTX, T> contextProvider,
            int nestingLevel) {
        return getExecutor().executeByHistoryNested(selectedEdge, contextProvider, nestingLevel);
    }

    @Override
    public MgcGraphExecutionResult executeAnyNode(MgcNode<CTX, T> currentNode, String selectedEdge,
            MgcCtxProvider<CTX, T> contextProvider) {
        return getExecutor().executeAnyNode(currentNode, selectedEdge, contextProvider);
    }

    @Override
    public MgcGraphExecutionResult executeAnyNodeNested(MgcNode<CTX, T> currentNode, String selectedEdge, int nestingLevel,
            MgcCtxProvider<CTX, T> contextProvider) {
        return getExecutor().executeAnyNodeNested(currentNode, selectedEdge, nestingLevel, contextProvider);
    }
}
