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

import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.Arrays;
import java.util.List;

public interface KvKeyEnum extends KvKey3Categories {
    @Override
    default List<String> categories() {
        return Arrays.asList(name().split("_"));
    }

    String name();

    @Override
    default String getKey1() {
        return Cc.getAt(categories(), 0)
                .orElseThrow(() -> new RuntimeException("Bad name:" + name()));
    }

    @Override
    default O<String> getKey2() {
        return Cc.getAt(categories(), 1);
    }

    @Override
    default O<String> getKey3() {
        return Cc.getAt(categories(), 2);
    }
}
