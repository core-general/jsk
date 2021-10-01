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
import lombok.Setter;
import lombok.experimental.Accessors;

@SuppressWarnings({"unused", "UnusedReturnValue", "WeakerAccess", "Duplicates"})
@Accessors(chain = true)
public class MinMaxAvgWithObj<T> {
    @Getter
    @Setter
    double min = Double.MAX_VALUE;
    @Getter
    @Setter
    double max = -Double.MAX_VALUE;
    @Getter
    T minO = null;
    @Getter
    T maxO = null;

    double curAvg = 0d;
    @Getter
    long count = 0L;

    double last = 0L;
    T lastO = null;

    public MinMaxAvgWithObj<T> add(float value, T obj) {
        return add((double) value, obj);
    }

    public MinMaxAvgWithObj<T> add(double value, T obj) {
        if (value < min) {
            min = value;
            minO = obj;
        }
        if (value > max) {
            max = value;
            maxO = obj;
        }
        //https://math.stackexchange.com/questions/106700/incremental-averageing
        curAvg = curAvg + (value - curAvg) / (count + 1);
        count += 1;
        last = value;
        lastO = obj;
        return this;
    }

    public double getAvg() {
        return curAvg;
    }

    public MinMaxAvgWithObj<T> setAvg(float avg) {
        return setAvg((double) avg);
    }

    public MinMaxAvgWithObj<T> setAvg(double avg) {
        curAvg = avg;
        count = 1;
        last = curAvg;
        lastO = null;
        return this;
    }

    public boolean isValid() {
        return count > 0;
    }

    @Override
    public String toString() {
        return String.format("[min=%.3f, avg=%.3f, last=%.3f, max=%.3f, count=%d, sum=%d]", min, curAvg, last, max, count,
                (long) (count * curAvg));
    }
}
