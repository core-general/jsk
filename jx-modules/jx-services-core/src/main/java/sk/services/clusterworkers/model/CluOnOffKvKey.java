package sk.services.clusterworkers.model;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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
import sk.services.kv.keys.KvKey2Categories;
import sk.utils.functional.O;

@AllArgsConstructor
public class CluOnOffKvKey implements KvKey2Categories {
    String nameOfTarget;

    @Override
    public String getDefaultValue() {
        return "false";
    }

    @Override
    public String getKey1() {
        return "__ON_OFFS";
    }

    @Override
    public O<String> getKey2() {
        return O.of(nameOfTarget);
    }
}
