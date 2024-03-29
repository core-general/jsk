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

import sk.utils.functional.O;

public interface MgcGraphExecutionResult {
    default boolean isError() {
        return isCantFindEdge() || getResults().isError();
    }

    default O<Throwable> getError() {
        if (isCantFindEdge()) {
            return O.of(new RuntimeException("Can't find edge"));
        } else if (getResults().isError()) {
            return getResults().getError();
        }
        return O.empty();
    }

    boolean isCantFindEdge();

    /** Initial step - is when the first node is entered */
    boolean isInitialStep();

    /** Final node - is the node which doesn't have any normal edges out */
    boolean isReachedFinalNode();

    /**/
    void unreachFinalNode();

    MgcListenerResults getResults();
}
