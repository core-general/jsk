package sk.utils.computation.chained.values;

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

import sk.utils.computation.chained.ChainedComputation;
import sk.utils.functional.F1;
import sk.utils.javafixes.TypeWrap;

public class SimpleComputedValue<OUT> extends StaticValue<OUT> implements SomeValue<OUT> {
    final F1<ChainedComputation, OUT> applier;

    public SimpleComputedValue(F1<ChainedComputation, OUT> applier, Class<OUT> cls) {
        this(applier, TypeWrap.simple(cls));
    }

    public SimpleComputedValue(F1<ChainedComputation, OUT> applier, TypeWrap<OUT> cls) {
        super(null, cls);
        this.applier = applier;
    }

    @Override
    public synchronized OUT get(ChainedComputation ctx) {
        return value = (value == null ? applier.apply(ctx) : value);
    }
}
