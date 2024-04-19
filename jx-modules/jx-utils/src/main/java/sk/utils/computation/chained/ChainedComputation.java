package sk.utils.computation.chained;

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

import lombok.Getter;
import lombok.SneakyThrows;
import sk.utils.computation.chained.values.SomeValue;

import java.util.Collections;
import java.util.Map;

@Getter
public class ChainedComputation {
    private final Map<String, SomeValue<?>> dataHolder;
    private final boolean parallelComputation;

    public ChainedComputation(Map<String, SomeValue<?>> dataHolder, boolean parallelComputation) {
        this.dataHolder = Collections.unmodifiableMap(dataHolder);
        this.parallelComputation = parallelComputation;
    }

    public <OUT extends ChainedMappedInput> OUT calculate(Class<OUT> cls) {
        return createMappedInput(cls);
    }


    @SneakyThrows
    public <IN extends ChainedMappedInput> IN createMappedInput(Class<IN> cls) {
        return cls.getConstructor(ChainedComputation.class).newInstance(this);
    }
}
