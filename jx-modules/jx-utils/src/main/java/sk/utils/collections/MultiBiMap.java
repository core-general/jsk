package sk.utils.collections;

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
import sk.utils.statics.Cc;

import java.util.Map;
import java.util.Set;

@Getter
public class MultiBiMap<F, S> {
    Map<F, Set<S>> firstBySecond = Cc.m();
    Map<S, Set<F>> secondByFirst = Cc.m();

    public MultiBiMap(Map<F, S> map) {
        for (Map.Entry<F, S> fsEntry : map.entrySet()) {
            add(fsEntry.getKey(), fsEntry.getValue());
        }
    }

    public void add(F first, S second) {
        Cc.computeAndApply(firstBySecond, first, (k, v) -> Cc.add(v, second), Cc::s);
        Cc.computeAndApply(secondByFirst, second, (k, v) -> Cc.add(v, first), Cc::s);
    }

}
