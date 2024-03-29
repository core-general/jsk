package sk.utils.minmax;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Getter
public class MinMaxL {
    long min = Long.MAX_VALUE;
    long max = Long.MIN_VALUE;

    public MinMaxL add(MinMaxL value) {
        if (value.min < min) {
            min = value.min;
        }
        if (value.max > max) {
            max = value.max;
        }
        return this;
    }

    public MinMaxL add(long value) {
        if (value < min) {
            min = value;
        }
        if (value > max) {
            max = value;
        }
        return this;
    }

    public MinMaxL add(long... values) {
        for (long value : values) {
            add(value);
        }
        return this;
    }

    public boolean isValid() {
        return min != Long.MAX_VALUE && max != Long.MIN_VALUE;
    }

    @Override
    public String toString() {
        return String.format("[%d,%d]", min, max);
    }
}
