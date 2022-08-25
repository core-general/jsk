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
import lombok.extern.log4j.Log4j2;
import sk.utils.functional.C2;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.statics.Fu;
import sk.utils.statics.Re;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
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

                                    final O<F1<Object, Object>> getter = Re.getter(fld);
                                    final O<C2<Object, Object>> setter = Re.setter(fld);
                                    return Fu.bothPresent(getter, setter).map(gs -> {
                                        return new OFieldGetterSetter(fld, optionalOrO, gs.i1(), gs.i2());
                                    }).orElseGet(() -> {
                                        log.error("",
                                                new RuntimeException("Class:" + cls + " field:" + fld.getName() +
                                                        " does not have getter or setter!"));
                                        return null;
                                    });

                                })
                                .filter(Fu.notNull())
                                .toList());

                gettersAndSettersForEmpties.forEach($ -> $.makeNonNull(obj));
                return obj;
            }
        };
    }

    private record OFieldGetterSetter(Field fld, boolean optionalOrO, F1<Object, Object> getter, C2<Object, Object> setter) {
        public Object makeNonNull(Object in) {
            final Object apply = getter.apply(in);
            if (apply == null) {
                setter.accept(in, optionalOrO ? Optional.empty() : O.empty());
            }
            return in;
        }
    }
}
