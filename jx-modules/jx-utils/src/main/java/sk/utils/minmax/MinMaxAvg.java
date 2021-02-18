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

@SuppressWarnings({"unused", "UnusedReturnValue", "WeakerAccess"})
@Accessors(chain = true)
public class MinMaxAvg {
    @Getter
    @Setter
    double min = Double.MAX_VALUE;
    @Getter
    @Setter
    double max = -Double.MAX_VALUE;

    double curAvg = 0d;
    @Getter
    long count = 0L;

    double last = 0L;

    public MinMaxAvg add(float value) {
        if (value < min) {
            min = value;
        }
        if (value > max) {
            max = value;
        }
        curAvg = (curAvg * count + value) / (count + 1);
        count += 1;
        last = value;
        return this;
    }

    public MinMaxAvg add(double value) {
        if (value < min) {
            min = value;
        }
        if (value > max) {
            max = value;
        }
        curAvg = (curAvg * count + value) / (count + 1);
        count += 1;
        last = value;
        return this;
    }

    public MinMaxAvg add(MinMaxAvg value) {
        if (value.min < min) {
            min = value.min;
        }
        if (value.max > max) {
            max = value.max;
        }
        curAvg = (curAvg * count + value.getAvg() * value.getCount()) / (count + value.getCount());
        count += value.count;
        last = value.curAvg;
        return this;
    }

    public MinMaxAvg add(float... values) {
        for (float value : values) {
            add(value);
        }
        return this;
    }

    public MinMaxAvg add(double... values) {
        for (double value : values) {
            add(value);
        }
        return this;
    }

    public double getAvg() {
        return curAvg;
    }

    public MinMaxAvg setAvg(float avg) {
        curAvg = avg;
        count = 1;
        last = curAvg;
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

    public static MinMaxAvg zero() {
        return new MinMaxAvg().add(0);
    }
}
