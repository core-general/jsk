package sk.utils.math;

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
import lombok.ToString;

@ToString
public class KahanSum {
    // accumulator
    @Getter private double sum = 0.0;
    // error
    private double err = 0.0;

    public void add(double value) {
        double valWithNoError = value - err;
        double newSum = sum + valWithNoError;

        // Algebraically, err is always 0
        // when t is replaced by its
        // value from the above expression.
        // But, when there is a loss,
        // the higher-order y is cancelled
        // out by subtracting y from c and
        // all that remains is the
        // lower-order error in c
        err = (newSum - sum) - valWithNoError;

        sum = newSum;
    }
}
