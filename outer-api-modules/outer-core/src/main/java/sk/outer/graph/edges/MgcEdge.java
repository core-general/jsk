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
import sk.utils.ifaces.IdentifiableString;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;

import java.util.List;
import java.util.function.BiPredicate;

public interface MgcEdge extends MgcListenerProcessor, IdentifiableString, MgcParsedDataHolder {
    default boolean acceptEdge(String edgeId, MgcGraphExecutionContext context) {
        return getPossibleEdges(getParsedData().getText(), context).stream().anyMatch($ -> getAcceptPredicate().test($, edgeId))
                || getAcceptPredicate().test("!any_string_which_will_never_be_met_in_production!", edgeId);
    }

    default BiPredicate<String, String> getAcceptPredicate() {
        return Fu::equal;
    }

    default List<String> getPossibleEdges(String template, MgcGraphExecutionContext context) {
        return Cc.l(template);
    }
}
