package sk.outer.graph.listeners;

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

import lombok.AllArgsConstructor;
import sk.outer.graph.execution.MgcGraphExecutionContext;
import sk.outer.graph.parser.MgcTypeUtil;
import sk.utils.functional.F1;

@AllArgsConstructor
public class MgcDefaultListener
        <CTX extends MgcGraphExecutionContext<CTX, T>, T extends Enum<T> & MgcTypeUtil<T>, RES extends MgcListenerResult>
        implements MgcListener<CTX, T, RES> {
    String id;
    F1<CTX, RES> processor;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public RES apply(CTX mgcGraphExecutionContext) {
        return processor.apply(mgcGraphExecutionContext);
    }
}
