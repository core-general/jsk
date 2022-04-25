package sk.math.data.func;

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

public abstract class MFuncProto {
    public abstract double value(double[] x, double[] p);

    /*
    Jacobian - это матрица частных производных по каждой из переменных в vars.
    y=ax+b

    {a=2,b=3,c=6}
    {x=5}
    jacobian = {d(a*log(x+b)+c)/da, d(a*log(x+b)+c)/db, d(a*log(x+b)+c)/dc} =
    {log(x+b), a/(x+b), 1} = {log(5+3), 2/(5+3),1}
     */
    public abstract double[] jacobian(double[] x, double[] p);

    public abstract int paramCount();
}
