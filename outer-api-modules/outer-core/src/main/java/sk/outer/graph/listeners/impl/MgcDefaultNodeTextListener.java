package sk.outer.graph.listeners.impl;

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
import sk.outer.graph.listeners.MgcDefaultListener;
import sk.outer.graph.nodes.MgcNode;
import sk.outer.graph.parser.MgcTypeUtil;

public class MgcDefaultNodeTextListener
        <CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>>
        extends MgcDefaultListener<CTX, T, MgcNodeTextListenerResult> {
    public static final String id = "node_text";

    public MgcDefaultNodeTextListener(MgcNode<CTX, T> newNode) {
        super(id, context -> new MgcNodeTextListenerResult(newNode.getId(),
                newNode.getText(newNode.getParsedData().getText(), context)));
    }

}
