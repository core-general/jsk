package sk.utils.ifaces;

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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public interface Identifiable<T> {
    T getId();

    default String toStringer() {
        return Objects.toString(getId());
    }

    public static <ID, T extends Identifiable<ID>> Map<ID, T> getMapping(Collection<T> items) {
        return items.stream().collect(Cc.toM($ -> $.getId(), $ -> $));
    }
}
