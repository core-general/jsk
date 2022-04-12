package sk.outer.graph.parser;

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
import sk.outer.graph.edges.MgcEdge;
import sk.outer.graph.edges.MgcMetaEdge;
import sk.outer.graph.edges.MgcNormalEdge;
import sk.outer.graph.nodes.MgcNode;
import sk.outer.graph.nodes.MgcNodeBase;
import sk.utils.functional.F1;
import sk.utils.functional.O;

@AllArgsConstructor
public class MgcDefaultObjectGenerator implements MgcObjectGenerator {
    F1<MgcParsedData, MgcMetaEdge> metaEdge;
    F1<MgcParsedData, MgcNormalEdge> normalEdge;
    F1<MgcParsedData, MgcNode> node;

    public MgcDefaultObjectGenerator() {
        this(MgcMetaEdge::new, MgcNormalEdge::new, MgcNodeBase::new);
    }

    @Override
    public O<MgcEdge> getEdgeGenerator(MgcParsedData parsedData, boolean meta) {
        return O.of(meta ? metaEdge.apply(parsedData) : normalEdge.apply(parsedData));
    }

    @Override
    public O<MgcNode> getNodeGenerator(MgcParsedData parsedData) {
        return O.of(node.apply(parsedData));
    }
}
