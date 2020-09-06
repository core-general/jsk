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

/**
 * @author kivan
 */
@SuppressWarnings("unused")
@Getter
public class MinMaxWithObject<T> {
    int min = Integer.MAX_VALUE;
    T minO = null;
    int max = Integer.MIN_VALUE;
    T maxO = null;

    public MinMaxWithObject add(MinMaxWithObject<T> value) {
        if (value.min < min) {
            min = value.min;
            minO = value.getMinO();
        }
        if (value.max > max) {
            max = value.max;
            maxO = value.getMaxO();
        }
        return this;
    }

    public MinMaxWithObject add(int value, T object) {
        if (value < min) {
            min = value;
            minO = object;
        }
        if (value > max) {
            max = value;
            maxO = object;
        }
        return this;
    }

    @Override
    public String toString() {
        return String.format("[%d:%s,%d:%s]", min, minO, max, maxO);
    }
}
