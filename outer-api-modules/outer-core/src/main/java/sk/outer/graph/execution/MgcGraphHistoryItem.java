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

import lombok.AllArgsConstructor;
import lombok.Data;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;

import java.util.List;

@Data
@AllArgsConstructor
public class MgcGraphHistoryItem {
    public static final String type = "sk.outer.graph.execution.MgcGraphHistoryItem";

    String graphId;
    String graphVersion;

    boolean node;
    String id;
    String text;
    List<String> possibleEdges;
    List<String> possibleMetaEdges;

    public boolean isId(String otherId) {
        return Fu.equal(id, otherId);
    }

    public List<String> getAllEdges() {
        return Cc.addAll(Cc.l(), possibleEdges, possibleMetaEdges);
    }
}
