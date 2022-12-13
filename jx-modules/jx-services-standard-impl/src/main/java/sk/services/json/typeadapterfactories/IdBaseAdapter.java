package sk.services.json.typeadapterfactories;

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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.SneakyThrows;
import sk.utils.ids.IdBase;
import sk.utils.statics.Re;

import java.io.IOException;
import java.lang.reflect.Constructor;

public abstract class IdBaseAdapter<A extends Comparable<A>, T extends IdBase<A>> extends TypeAdapter<T> {
    final Constructor<T> constructor;

    @SneakyThrows
    public IdBaseAdapter() {
        Class<T> idBaseCls = (Class<T>) Re.getParentParameters(this.getClass()).get()[0];
        Class<A> parameterCls = (Class<A>) Re.getParentParameters(this.getClass()).get()[1];

        constructor = idBaseCls.getConstructor(parameterCls);
        constructor.setAccessible(true);
    }

    protected abstract A construct(String value);

    @Override
    public final void write(JsonWriter out, T value) throws IOException {
        if (value != null) {
            out.value(value.getId().toString());
        } else {
            out.value((String) null);
        }
    }

    @SneakyThrows
    @Override
    public final T read(JsonReader in) throws IOException {
        if (in.nextString() == null) {
            return null;
        }

        final A created = construct(in.nextString());
        return constructor.newInstance(created);
    }
}
