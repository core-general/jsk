package sk.outer.graph.edges;

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
import sk.outer.graph.parser.MgcParsedData;
import sk.outer.graph.parser.MgcTypeUtil;
import sk.utils.functional.F1;
import sk.utils.functional.O;

public class MgcMetaEdge<CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>>
        extends MgcEdgeBase<CTX, T> {
    private F1<CTX, O<String>> getPossibleNodeIdIfMetaBackWrapper;

    public MgcMetaEdge(MgcParsedData<T> parsedData) {
        this(parsedData, ctx -> O.empty());
    }

    public MgcMetaEdge(MgcParsedData<T> parsedData, F1<CTX, O<String>> getPossibleNodeIdIfMetaBackWrapper) {
        super(parsedData);
        this.getPossibleNodeIdIfMetaBackWrapper = getPossibleNodeIdIfMetaBackWrapper;
    }

    public O<String> getPossibleNodeIdIfMetaBack(CTX context) {
        return getPossibleNodeIdIfMetaBackWrapper.apply(context);
    }
}
