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

import sk.outer.graph.edges.MgcEdge;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.nodes.MgcNode;
import sk.utils.functional.F1;

public interface MgcObjectGenerator<CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>> {
    MgcEdge<CTX, T> getEdgeGenerator(MgcParsedData<T> parsedData);

    MgcNode<CTX, T> getNodeGenerator(MgcParsedData<T> parsedData);


    public static <CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>>
    MgcObjectGenerator<CTX, T> node(F1<MgcParsedData<T>, MgcNode<CTX, T>> processor) {
        return (MgcNodeGenerator<CTX, T>) (pd) -> processor.apply(pd);
    }

    public static <CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>>
    MgcObjectGenerator<CTX, T> edge(F1<MgcParsedData<T>, MgcEdge<CTX, T>> processor) {
        return (MgcEdgeGenerator<CTX, T>) (pd) -> processor.apply(pd);
    }
}
