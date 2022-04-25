package sk.math.data.estimate;

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

import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.math.data.func.MFuncImpl;
import sk.math.data.func.MFuncProto;

import java.util.Arrays;

@AllArgsConstructor
@Getter
public class MOptimizeInfo<T extends MFuncProto> {
    MFuncImpl<T> optimizedFunction;
    double squareRootError;

    @Override
    public String toString() {
        return String.format("Err=%.2f", squareRootError) + " " + Arrays.toString(optimizedFunction.getParams());
    }
}
