package sk.utils.javafixes;

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
import lombok.Getter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@SuppressWarnings("unused")
@Getter
@EqualsAndHashCode
public class TypeWrap<T> {
    private Type type;

    public static TypeWrap<?> raw(Type t) {
        TypeWrap<Object> objectTypeWrap = new TypeWrap<>();
        objectTypeWrap.type = t;
        return objectTypeWrap;
    }

    public static <A> TypeWrap<A> simple(Class<A> simple) {
        TypeWrap<A> typeToken = new TypeWrap<>();
        typeToken.type = simple;
        return typeToken;
    }

    public static <T, A> TypeWrap getHolder(Class<A> holder, Class<T> clasInHolder) {
        TypeWrap<A> typeToken = new TypeWrap<>();
        typeToken.type = new CustomType(holder, new Type[]{clasInHolder});
        return typeToken;
    }

    public static <A> TypeWrap getCustom(Class<A> holder, Class<?>... other) {
        TypeWrap<A> typeToken = new TypeWrap<>();
        typeToken.type = new CustomType(holder, other);
        return typeToken;
    }

    public static <T, CC extends Collection<T>> TypeWrap<CC> getCollection(Class<CC> collection,
            Class<T> classInCollection) {
        TypeWrap<CC> typeToken = new TypeWrap<>();
        typeToken.type = new CustomType(collection, new Type[]{classInCollection});
        return typeToken;
    }

    public static <T> TypeWrap<List<T>> getList(Class<T> classInCollection) {
        TypeWrap<List<T>> typeToken = new TypeWrap<>();
        typeToken.type = new CustomType(List.class, new Type[]{classInCollection});
        return typeToken;
    }

    public static <T> TypeWrap<Set<T>> getSet(Class<T> classInSet) {
        TypeWrap<Set<T>> typeToken = new TypeWrap<>();
        typeToken.type = new CustomType(Set.class, new Type[]{classInSet});
        return typeToken;
    }


    public static <T1, T2, D extends Map, C extends Map<T1, T2>> TypeWrap<C> getMap(Class<D> map, Class<T1> keyClass,
            Class<T2> valueClass) {
        TypeWrap<C> typeToken = new TypeWrap<>();
        typeToken.type = new CustomType(map, new Type[]{keyClass, valueClass});
        return typeToken;
    }

    public static <T1, T2, C extends Map<T1, T2>> TypeWrap<C> getMap(Class<T1> keyClass,
            Class<T2> valueClass) {
        TypeWrap<C> typeToken = new TypeWrap<>();
        typeToken.type = new CustomType(HashMap.class, new Type[]{keyClass, valueClass});
        return typeToken;
    }

    @Data
    public static class CustomType implements ParameterizedType {
        Type rawType;
        Type[] actualTypeArguments;
        Type ownerType;

        CustomType(Type rawType, Type[] actualTypes) {
            this.rawType = rawType;
            this.actualTypeArguments = actualTypes;
        }
    }

}
