package sk.utils.math;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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
import lombok.Getter;
import sk.utils.statics.Ma;
import sk.utils.statics.St;

@AllArgsConstructor
public class MilliNum {
    @Getter
    long multValue;

    /**
     * value with dot eg: 10.05 or without like 163
     *
     * @param realValue
     */
    private MilliNum(String realValue) {
        final String[] split = realValue.split("\\.");
        final long[] vv = new long[2];
        vv[0] = Ma.pl(split[0]) * 100;
        if (split.length > 1) {
            String rightPart = St.ss(split[1], 0, 2);
            if (rightPart.length() == 1) {
                rightPart = rightPart + "0";
            }
            vv[1] = Ma.pl(rightPart);
        }
        this.multValue = vv[0] + vv[1];
    }

    public static MilliNum create(String milliVal) {
        return new MilliNum(milliVal);
    }

    public static MilliNum createByRaw(long multipliedVal) {
        return new MilliNum(multipliedVal);
    }

    public String toNonMilli() {
        String l = multValue % 100 + "";
        return multValue / 100 + "." + (l.length() == 2 ? l : "0" + l);
    }
}
