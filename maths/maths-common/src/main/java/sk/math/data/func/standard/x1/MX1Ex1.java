package sk.math.data.func.standard.x1;

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

import sk.math.data.func.MFuncProto;

/** y = (e^(ax+b))+c; */
public class MX1Ex1 extends MFuncProto {
    @Override
    public double value(double[] x, double[] p) {
        return Math.exp(p[0] * x[0] + p[1]) + p[2];
    }

    @Override
    public double[] jacobian(double[] x, double[] p) {
        final double exp = Math.exp(p[0] * x[0] + p[1]);
        return new double[]{
                x[0] * exp,
                exp,
                1
        };
    }

    @Override
    public int paramCount() {
        return 3;
    }
}
