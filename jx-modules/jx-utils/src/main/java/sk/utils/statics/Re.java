package sk.utils.statics;

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

import sk.utils.functional.F1;
import sk.utils.functional.O;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@SuppressWarnings({"unused"})
public final class Re {

    public static <T> O<T> createObjectByDefault(Class<T> claz) {
        try {
            return O.of(claz.getDeclaredConstructor().newInstance());
        } catch (Throwable e) {
            return O.empty();
        }
    }

    public static <T extends Enum<T>> O<T> findInEnum(Class<T> enumCls, String enumName) {
        try {
            return O.of(Enum.valueOf(enumCls, enumName));
        } catch (Exception e) {
            return O.empty();
        }
    }

    public static O<Class<?>> getClassIfExist(String className) {
        try {
            return O.ofNull(Class.forName(className));
        } catch (ClassNotFoundException e) {
            return O.empty();
        }
    }


    //region fields
    public static SortedSet<Field> getNonStaticPublicFields(Class target) {
        return getAllNonStaticFields(new TreeSet<>(Comparator.comparing(Field::getName)), target, Class::getFields);
    }

    public static SortedSet<Field> getAllNonStaticFields(Class target) {
        return getAllNonStaticFields(new TreeSet<>(Comparator.comparing(Field::getName)), target, Class::getDeclaredFields);
    }

    public static boolean isStatic(Field f) {
        return Modifier.isStatic(f.getModifiers());
    }
    //endregion


    //region Proxies
    @SuppressWarnings("unchecked")
    public static <T> T singleProxy(Class<T> cls, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls}, handler);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2> T1 doubleProxy(Class<T1> cls, Class<T2> cl2, InvocationHandler handler) {
        return (T1) Proxy.newProxyInstance(cls.getClassLoader(), new Class[]{cls, cl2}, handler);
    }
    //endregion


    //region private
    private static SortedSet<Field> getAllNonStaticFields(SortedSet<Field> fields, Class<?> type,
            F1<Class<?>, Field[]> clsToFields) {
        fields.addAll(Stream.of(clsToFields.apply(type)).filter(f -> !isStatic(f)).collect(toList()));

        if (type.getSuperclass() != null) {
            fields = getAllNonStaticFields(fields, type.getSuperclass(), clsToFields);
        }

        return fields;
    }

    private Re() {
    }
    //endregion
}
