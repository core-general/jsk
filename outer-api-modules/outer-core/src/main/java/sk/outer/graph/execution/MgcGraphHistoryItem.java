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

import lombok.Data;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;

import java.util.ArrayList;
import java.util.List;

@Data
public class MgcGraphHistoryItem {
    public static final String type = "sk.outer.graph.execution.MgcGraphHistoryItem";

    final List<MgcNestingInfo> nestedGraphInfo;

    final boolean node;
    final String id;
    final String text;
    final List<String> possibleEdges;
    final List<String> possibleMetaEdges;

    private MgcGraphHistoryItem(List<MgcNestingInfo> nestedGraphInfo, boolean node, String id, String text,
            List<String> possibleEdges, List<String> possibleMetaEdges) {
        this.nestedGraphInfo = nestedGraphInfo;
        this.node = node;
        this.id = id;
        this.text = text;
        this.possibleEdges = possibleEdges;
        this.possibleMetaEdges = possibleMetaEdges;
    }

    public boolean isId(String otherId) {
        return Fu.equal(id, otherId);
    }

    public List<String> getAllEdges() {
        return Cc.addAll(Cc.l(), possibleEdges, possibleMetaEdges);
    }

    public int getNestingLevel() {
        return nestedGraphInfo.size() - 1;
    }

    public MgcGraphHistoryItem withReplacedEdges(List<String> possibleEdges, List<String> possibleMetaEdges) {
        return new MgcGraphHistoryItem(
                nestedGraphInfo, node, id, text, possibleEdges, possibleMetaEdges
        );
    }

    public static MgcGraphHistoryItem newItem(
            int currentNestingLevel,
            O<MgcGraphHistoryItem> olastNode,
            MgcGraphInfo graphInfo, boolean node, String id,
            String text, List<String> possibleEdges, List<String> possibleMetaEdges) {

        List<MgcNestingInfo> newNesting;
        if (olastNode.isEmpty()) {
            newNesting = Cc.l(new MgcNestingInfo(graphInfo, O.empty()));
        } else {
            final MgcGraphHistoryItem lastNode = olastNode.get();
            if (lastNode.getNestingLevel() < currentNestingLevel) {
                //going into deep
                newNesting = Cc.add(new ArrayList<>(lastNode.getNestedGraphInfo()),
                        new MgcNestingInfo(graphInfo, O.of(lastNode.getId())));
            } else if (lastNode.getNestingLevel() == currentNestingLevel) {
                newNesting = lastNode.getNestedGraphInfo();
            } else {
                newNesting = lastNode.getNestedGraphInfo()
                        .subList(0, lastNode.getNestedGraphInfo().size() - (lastNode.getNestingLevel() - currentNestingLevel));
            }
        }

        return new MgcGraphHistoryItem(newNesting, node, id, text, possibleEdges, possibleMetaEdges);
    }
}
