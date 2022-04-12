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
import sk.utils.statics.Cc;

import java.util.List;
import java.util.function.BiPredicate;

public class MgcAnyTextEdge extends MgcNormalEdge {
    public MgcAnyTextEdge(MgcParsedData parsedData) {super(parsedData);}

    @Override
    public BiPredicate<String, String> getAcceptPredicate() {
        return (_1, _2) -> true;
    }

    @Override
    public List<String> getPossibleEdges(String template, MgcGraphExecutionContext context) {
        return Cc.l();
    }
}

