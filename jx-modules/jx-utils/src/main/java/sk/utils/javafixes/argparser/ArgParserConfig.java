package sk.utils.javafixes.argparser;

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


import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.util.Comparator;
import java.util.TreeSet;

public interface ArgParserConfig<T extends ArgParserConfig<T>> extends Comparable<T> {
    /** If empty - it could have no prefixes, but it must be only one parameter like this */
    O<TreeSet<String>> getCommandPrefix();

    boolean isRequired();

    String getDescription();

    default public String asString() {
        return getCommandPrefix().map(
                        $ -> (isRequired() ? "Required argument: " : "Argument: ") + "\"" + Cc.join("\", \"", $) + "\" desc: " +
                                getDescription())
                .orElseGet(() -> (isRequired() ? "Required argument(no-arg): " : "Argument(no-arg): ") + "desc: " +
                        getDescription());
    }

    @Override
    default int compareTo(T o) {
        return Comparator
                .<T, String>comparing(k1 -> k1.isRequired() ? "0" : "1")
                .thenComparing(k1 -> k1.getCommandPrefix()
                        .flatMap($ -> $.size() > 0
                                      ? O.of($.first())
                                      : O.empty()).orElse("___"))
                .thenComparing(k1 -> k1.getDescription()).compare(o, (T) this);
    }
}
