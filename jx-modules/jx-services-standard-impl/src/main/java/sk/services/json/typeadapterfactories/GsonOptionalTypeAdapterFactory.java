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

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.extern.slf4j.Slf4j;
import sk.utils.functional.C2;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.statics.Ex;
import sk.utils.statics.Fu;
import sk.utils.statics.Re;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class GsonOptionalTypeAdapterFactory implements TypeAdapterFactory {
    final ConcurrentHashMap<Class<?>, List<OFieldGetterSetter>> optionalFieldsChecker = new ConcurrentHashMap<>();

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        final TypeAdapter<T> delegateAdapter = gson.getDelegateAdapter(this, type);
        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegateAdapter.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                var obj = delegateAdapter.read(in);

                if (obj == null) {
                    return null;
                }

                final List<OFieldGetterSetter> gettersAndSettersForEmpties = optionalFieldsChecker.computeIfAbsent(obj.getClass(),
                        (cls) -> Re.getAllNonStaticFields(cls).stream()
                                .map(fld -> {
                                    Boolean optionalOrO = ((fld.getType() == O.class)
                                                           ? (Boolean) false
                                                           : (fld.getType() == Optional.class)
                                                             ? (Boolean) true
                                                             : (Boolean) null);
                                    if (optionalOrO == null) {
                                        return null;
                                    }

                                    final F1<Object, Object> getter = Re.getter(fld);
                                    final O<C2<Object, Object>> setter = Re.setter(fld);
                                    if (cls.isRecord()) {
                                        return new OFieldGetterSetter(fld, optionalOrO, getter, O.empty());
                                    } else {
                                        return setter.map(gs -> new OFieldGetterSetter(fld, optionalOrO, getter, O.of(gs)))
                                                .orElseGet(() -> Ex.thRow(
                                                        "Field %s of %s should not be final!".formatted(fld.getName(),
                                                                fld.getDeclaringClass())));
                                    }
                                })
                                .filter(Fu.notNull())
                                .toList());

                gettersAndSettersForEmpties.forEach($ -> $.makeNonNull(obj));
                return obj;
            }
        };
    }

    private record OFieldGetterSetter(Field fld, boolean optionalOrO, F1<Object, Object> getter, O<C2<Object, Object>> setter) {
        public Object makeNonNull(Object in) {
            final Object apply = getter.apply(in);
            if (apply == null) {
                if (setter.isPresent()) {
                    setter.get().accept(in, optionalOrO ? Optional.empty() : O.empty());
                } else {
                    return Ex.thRow("Field %s of %s should not be final because it is not a record!".formatted(fld.getName(),
                            fld.getDeclaringClass()));
                }
            }
            return in;
        }
    }
}
