package sk.services.json.typeadapterfactories;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2024 Core General
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
import lombok.SneakyThrows;
import sk.utils.functional.C2;
import sk.utils.functional.F1;
import sk.utils.functional.F1E;
import sk.utils.functional.OneOf;
import sk.utils.ids.IdBase;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GsonIdBaseTypeAdapterFactory implements TypeAdapterFactory {

    private final ConcurrentMap<Class, C2<IdBase, JsonWriter>> serializerOfIdBase = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class, F1<JsonReader, IdBase>> deserializerOfIdBase = new ConcurrentHashMap<>();


    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (IdBase.class.isAssignableFrom(type.getRawType())) {
            return (TypeAdapter<T>) new IdBaseTypeAdapter((TypeToken<IdBase>) type);
        } else {
            return gson.getDelegateAdapter(this, type);
        }
    }

    private class IdBaseTypeAdapter extends TypeAdapter<IdBase> {

        private final TypeToken<IdBase> type;
        private final Class<?> typeOfId;
        private final F1<Object, IdBase> objectCreator;

        @SneakyThrows
        public IdBaseTypeAdapter(TypeToken<IdBase> type) {
            this.type = type;
            typeOfId = findTypeOfIdField(type.getRawType());
            OneOf<Constructor, Constructor> leftOneParamRightNoParams =
                    Arrays.stream(this.type.getRawType().getConstructors()).filter($ -> $.getParameters().length == 1).findAny()
                            .<OneOf<Constructor, Constructor>>map($ -> OneOf.left($))
                            .or(() -> Arrays.stream(this.type.getRawType().getConstructors())
                                    .filter($ -> $.getParameters().length == 0)
                                    .findAny().map($ -> OneOf.right($)))
                            .orElseThrow(() -> new RuntimeException(type + " has problems with constructors"));

            objectCreator = o -> {
                TypeToken<IdBase> type1 = this.type;
                return leftOneParamRightNoParams.collect(
                        oneParamConstructor -> {
                            try {
                                return (IdBase) oneParamConstructor.newInstance(o);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        },
                        zeroParamConstructor -> {
                            try {
                                IdBase idBase = (IdBase) zeroParamConstructor.newInstance();
                                idBase.setId((Comparable) o);
                                return idBase;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
            };
        }

        private Class<?> findTypeOfIdField(Class<? super IdBase> rawType) {
            Type genericSuperclass = rawType.getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType pt && pt.getRawType() == IdBase.class) {
                return (Class) pt.getActualTypeArguments()[0];
            } else {
                return findTypeOfIdField(rawType.getSuperclass());
            }
        }

        @Override
        @SneakyThrows
        public void write(JsonWriter out, IdBase value) throws IOException {
            serializerOfIdBase.computeIfAbsent(value.getClass(), cls -> {
                try {
                    if (Integer.class.isAssignableFrom(typeOfId) || Long.class.isAssignableFrom(typeOfId)) {
                        return (idBase, jsonWriter) -> {
                            try {
                                jsonWriter.value(((Number) idBase.getId()).longValue());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        };
                    } else if (String.class.isAssignableFrom(typeOfId)) {
                        return (idBase, jsonWriter) -> {
                            try {
                                jsonWriter.value(((String) idBase.getId()));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        };
                    } else if (UUID.class.isAssignableFrom(typeOfId)) {
                        return (idBase, jsonWriter) -> {
                            try {
                                jsonWriter.value(((UUID) idBase.getId()).toString());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        };
                    } else {
                        throw new RuntimeException("Unknown class of IdBase id:" + typeOfId);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).accept(value, out);
        }

        @Override
        public IdBase read(JsonReader in) throws IOException {
            return deserializerOfIdBase.computeIfAbsent(type.getRawType(), cls -> {
                if (Integer.class.isAssignableFrom(typeOfId)) {
                    return (reader) -> {
                        try {
                            return objectCreator.apply(this.<Integer>getPlainIdDataOrWrapped(reader, r -> r.nextInt()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    };
                } else if (Long.class.isAssignableFrom(typeOfId)) {
                    return (reader) -> {
                        try {
                            return objectCreator.apply(this.<Long>getPlainIdDataOrWrapped(reader, r -> r.nextLong()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    };
                } else if (String.class.isAssignableFrom(typeOfId)) {
                    return (reader) -> {
                        try {
                            return objectCreator.apply(this.<String>getPlainIdDataOrWrapped(reader, r -> r.nextString()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    };
                } else if (UUID.class.isAssignableFrom(typeOfId)) {
                    return (reader) -> {
                        try {
                            return objectCreator.apply(
                                    UUID.fromString(this.<String>getPlainIdDataOrWrapped(reader, r -> r.nextString())));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    };
                } else {
                    throw new RuntimeException("Unknown class of IdBase id:" + typeOfId);
                }
            }).apply(in);
        }

        @SneakyThrows
        private <T> T getPlainIdDataOrWrapped(JsonReader reader, F1E<JsonReader, T> getVal) throws IOException {
            T nextVal;
            if ("begin_object".equalsIgnoreCase(reader.peek().name())) {
                reader.beginObject();
                reader.nextName();
                nextVal = getVal.apply(reader);
                reader.endObject();
            } else {
                nextVal = getVal.apply(reader);
            }
            return nextVal;
        }
    }
}
