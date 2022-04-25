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

import sk.outer.graph.listeners.MgcListenerProcessorResultImpl;
import sk.outer.graph.nodes.MgcGraph;
import sk.outer.graph.nodes.MgcNode;
import sk.utils.functional.O;
import sk.utils.paging.SimplePage;

import java.util.List;

public interface MgcGraphExecutionContext {
    default List<MgcGraphHistoryItem> getGraphHistoryDescending(int lastX) {
        return getGraphHistory(lastX, O.empty(), false, MgcObjectType.BOTH).getData();
    }

    default List<MgcGraphHistoryItem> getGraphHistoryDescending(int lastX, MgcObjectType type) {
        return getGraphHistory(lastX, O.empty(), false, type).getData();
    }

    SimplePage<MgcGraphHistoryItem, String> getGraphHistory(int count, O<String> npa,
            boolean ascending, MgcObjectType type);

    MgcGraph getExecutedGraph();

    MgcNode getFromNode();

    MgcNode getToNode();

    void setToNode(MgcNode toNode);

    String getSelectedEdge();

    void addGraphHistoryItem(MgcGraphHistoryItem item);

    MgcListenerProcessorResultImpl getEdgeProcessor();

    MgcListenerProcessorResultImpl getNodeProcessor();
}
