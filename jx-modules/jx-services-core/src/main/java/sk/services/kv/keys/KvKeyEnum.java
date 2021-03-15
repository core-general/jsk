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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface KvKeyEnum extends KvKey3Categories {
    @Override
    default List<String> categories() {
        final String[] names = name().split("_");
        switch (names.length) {
            case 0:
                return Collections.emptyList();
            case 1:
            case 2:
                return Arrays.asList(names);
            default:
                return Cc.l(names[0], names[1], Cc.stream(Arrays.copyOfRange(names, 2, names.length, String[].class))
                        .collect(Collectors.joining("_")));
        }
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
