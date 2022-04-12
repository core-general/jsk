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
        return isCantFindEdge()
                || getContext().getEdgeProcessor().isError()
                || getContext().getNodeProcessor().isError();
    }

    default O<Throwable> getError() {
        if (isCantFindEdge()) {
            return O.of(new RuntimeException("Can't find edge"));
        } else if (getContext().getEdgeProcessor().isError()) {
            return getContext().getEdgeProcessor().getError();
        } else if (getContext().getNodeProcessor().isError()) {
            return getContext().getNodeProcessor().getError();
        }
        return O.empty();
    }

    boolean isCantFindEdge();

    MgcGraphExecutionContext getContext();
}
