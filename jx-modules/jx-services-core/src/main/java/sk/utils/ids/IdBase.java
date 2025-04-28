package sk.utils.ids;

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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import sk.utils.IHaikunable;
import sk.utils.ifaces.Identifiable;
import sk.utils.statics.Fu;

import java.io.Serializable;

@SuppressWarnings("WeakerAccess")
@Data
@EqualsAndHashCode(of = {"id"})
@NoArgsConstructor(onConstructor_ = @Deprecated)
public abstract class IdBase<T extends Comparable<T>>
        implements Serializable, Comparable<IdBase<T>>, IHaikunable<T>, Identifiable<T> {
    protected T id;

    public IdBase(T id) {
        this.id = id;
    }

    @Override
    public final String toString() {
        return toStringer();
    }

    public int compareTo(IdBase<T> o) {
        return Fu.compare(getId(), o.getId());
    }
}
