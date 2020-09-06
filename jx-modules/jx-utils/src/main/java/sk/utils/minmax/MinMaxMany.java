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

import sk.utils.statics.Cc;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class MinMaxMany {
    private final Map<String, MinMax> minMaxes = new HashMap<>();

    public void add(String id, int value) {
        MinMax minMax = Cc.computeAndApply(minMaxes, id, (k, v) -> v, MinMax::new);
        minMax.add(value);
    }

    public MinMax get(String id) {
        return minMaxes.get(id);
    }
}
