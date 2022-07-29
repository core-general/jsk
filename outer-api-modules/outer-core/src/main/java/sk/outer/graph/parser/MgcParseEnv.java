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

import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.utils.functional.O;

public interface MgcParseEnv<CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>> {
    MgcObjectGenerator<CTX, T> generatorByType(T type);

    default MgcObjectGenerator<CTX, T> getGenerator(O<T> type) {
        return type.map($ -> generatorByType($)).orElseGet(this::getDefaultGenerator);
    }

    default MgcObjectGenerator<CTX, T> getDefaultGenerator() {
        return new MgcDefaultObjectGenerator<>();
    }

    default int maxSizeOfEdgeText() {
        return 70;
    }

    default boolean isLongEdgeSizeOk(MgcParsedData<T> mgcParsedData) {
        return false;
    }
}
