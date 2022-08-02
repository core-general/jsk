package sk.outer.graph;

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

import sk.exceptions.NotImplementedException;
import sk.outer.graph.execution.MgcGraphHistoryItem;
import sk.outer.graph.execution.MgcObjectType;
import sk.outer.graph.nodes.MgcNode;
import sk.utils.functional.O;
import sk.utils.paging.SimplePage;
import sk.utils.statics.St;

import java.util.List;
import java.util.stream.Collectors;

public interface MgcHistoryProvider {
    void addGraphHistoryItem(MgcGraphHistoryItem item);

    SimplePage<MgcGraphHistoryItem, String> getHistory(int count, O<String> npa,
            boolean ascending, MgcObjectType type);

    void replaceLastItemWith(MgcGraphHistoryItem mgcGraphHistoryItem);

    default boolean isFirstTimeOnThisNestingLevelWhenGoingDown(int currentNestingLvl) {
        return getLastNode().map($ -> {
            final int lastNestingLevel = $.getNestingLevel();
            if (lastNestingLevel < currentNestingLvl) {
                //going deep
                return true;
            } else {
                //same level or going up
                return false;
            }
        }).orElse(true);
    }

    default void flush() {
        /*
         in case storage is immutable and since we want replaceLastItemWith to work it's possible to flush data to storage
         only after all listeners have worked
         */
    }

    default void clearHistory() {
        throw new NotImplementedException();
    }

    default void removeHistoryItem(MgcGraphHistoryItem item) {
        throw new NotImplementedException();
    }

    default List<MgcGraphHistoryItem> getHistoryDescending(int lastX) {
        return getHistory(lastX, O.empty(), false, MgcObjectType.BOTH).getData();
    }

    default List<MgcGraphHistoryItem> getHistoryDescending(int lastX, MgcObjectType type) {
        return getHistory(lastX, O.empty(), false, type).getData();
    }

    default O<MgcGraphHistoryItem> getLastNode() {
        List<MgcGraphHistoryItem> data = getHistory(1, O.empty(), false, MgcObjectType.NODE).getData();
        if (data.size() == 0 || !data.get(0).isNode()) {
            return O.empty();
        } else {
            return O.of(data.get(0));
        }
    }

    default O<MgcGraphHistoryItem> getLastEdge() {
        List<MgcGraphHistoryItem> data = getHistory(1, O.empty(), false, MgcObjectType.EDGE).getData();
        if (data.size() == 0 || data.get(0).isNode()) {
            return O.empty();
        } else {
            return O.of(data.get(0));
        }
    }

    default String getCurrentNodeIdWithNesting(int currentNestingLevel, MgcNode<?, ?> curNode) {
        return getLastNode()
                .map($ -> {
                    final int lastNestingLevel = $.getNestingLevel();

                    String addCurLevel =
                            currentNestingLevel > lastNestingLevel ? $.getId() + "->" : "";

                    final String toJoin = $.getNestedGraphInfo().stream()
                            .limit(currentNestingLevel + 1)
                            .filter($$ -> $$.getPreviousLevelNestedGraphNodeId().isPresent())
                            .map($$ -> $$.getPreviousLevelNestedGraphNodeId().get())
                            .collect(Collectors.joining("->"));
                    return toJoin + (St.isNullOrEmpty(toJoin) ? "" : "->") + addCurLevel;
                })
                .orElse("") + curNode.getId();
    }
}
