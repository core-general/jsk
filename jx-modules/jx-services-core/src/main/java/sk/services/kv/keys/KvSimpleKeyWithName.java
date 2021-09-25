package sk.services.kv.keys;

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

import lombok.Getter;
import lombok.experimental.Accessors;
import sk.utils.functional.F0;

public
class KvSimpleKeyWithName implements KvKeyEnum {
    @Getter
    @Accessors(fluent = true)
    String name;
    String defaultValue;
    F0<String> defaultValueProvider;

    public KvSimpleKeyWithName(String name, String defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public KvSimpleKeyWithName(String name, F0<String> defaultValueProvider) {
        this.name = name;
        this.defaultValueProvider = defaultValueProvider;
    }

    public String getDefaultValue() {
        return defaultValue != null ? defaultValue : defaultValueProvider.get();
    }
}
