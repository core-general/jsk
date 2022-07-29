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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.nodes.MgcGraphExecutor;
import sk.outer.graph.nodes.MgcNode;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MgcCtxProvider<CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>> {
    private OneOf<CTX, MgcGraphExecutionContextGenerator<CTX, T>> data;

    public static <CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>>
    MgcCtxProvider<CTX, T> context(CTX value) {return new MgcCtxProvider<>(OneOf.left(value));}

    public static <CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>>
    MgcCtxProvider<CTX, T> generator(MgcGraphExecutionContextGenerator<CTX, T> generator) {
        return new MgcCtxProvider<>(OneOf.right(generator));
    }

    public CTX getContext(MgcGraphExecutor<CTX, T> g, O<MgcNode<CTX, T>> fromNode, O<String> edge) {
        final CTX collect = data.collect($ -> $, $ -> $.getCtx(g, fromNode, edge));
        data = OneOf.left(collect);
        return collect;
    }
}
