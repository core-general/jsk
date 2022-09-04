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

import sk.outer.graph.MgcParsedDataHolder;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.listeners.MgcListenerProcessor;
import sk.outer.graph.parser.MgcTypeUtil;
import sk.utils.ifaces.IdentifiableString;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;

import java.util.List;
import java.util.function.BiPredicate;

public interface MgcEdge<CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>>
        extends MgcListenerProcessor<CTX, T>, IdentifiableString, MgcParsedDataHolder<T> {
    default boolean acceptEdge(String userText, CTX context) {
        return getPossibleEdges(getParsedData().getText(), context).stream()
                .anyMatch($ -> getAcceptPredicate(context).test($, userText))
                //case when we work with any text
                || getAcceptPredicate(context).test("!any_string_which_will_never_be_met_in_production!", userText);
    }

    default BiPredicate<String, String> getAcceptPredicate(CTX context) {
        return Fu::equal;
    }

    default List<String> getPossibleEdges(String template, CTX context) {
        return Cc.l(template);
    }
}
