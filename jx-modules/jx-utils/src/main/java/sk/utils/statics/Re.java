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

import lombok.SneakyThrows;
import sk.utils.functional.C2;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.ifaces.Identifiable;
import sk.utils.ifaces.IdentifiableString;
import sk.utils.javafixes.FieldAccessor;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@SuppressWarnings({"unused"})
public final class Re/*flections*/ {
    @SneakyThrows
    public Class<?> cls(String clsName) {
        return Class.forName(clsName);
    }

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

    public static <T extends Enum<T> & IdentifiableString> O<T> findInEnumById(String text, Class<T> cls) {
        return O.of(Cc.stream(cls.getEnumConstants())
                .filter($ -> Fu.equal(text.trim().toLowerCase(), $.getId().toLowerCase()))
                .findAny());
    }

    public static <T extends Enum<T> & Identifiable<Integer>> O<T> findInEnumById(int integer, Class<T> cls) {
        return O.of(Cc.stream(cls.getEnumConstants())
                .filter($ -> Fu.equal(integer, $.getId()))
                .findAny());
    }

    public static O<Class<?>> getClassIfExist(String className) {
        try {
            return O.ofNull(Class.forName(className));
        } catch (ClassNotFoundException e) {
            return O.empty();
        }
    }

    public static O<Class<?>> getFirstParentParameter(Class<?> curClass) {
        return getParentParameters(curClass)
                .map($ -> $[0])
                .filter($ -> $ instanceof Class)
                .map($ -> (Class<?>) $);
    }

    private static final ConcurrentMap<Class<?>, O<Type[]>> parentParams = new ConcurrentHashMap<>();

    public static O<Type[]> getParentParameters(Class<?> curClass) {
        return parentParams.computeIfAbsent(curClass, (cls) -> O.ofNull(cls.getGenericSuperclass())
                .flatMap($ -> $ instanceof ParameterizedType
                              ? O.ofNull(((ParameterizedType) $).getActualTypeArguments())
                              : O.empty())
                .or(() -> cls.getSuperclass() != null
                          ? getParentParameters(cls.getSuperclass())
                          : O.<Type[]>empty()));
    }

    //region fields
    public static SortedSet<Field> getNonStaticPublicFields(Class<?> target) {
        return getAllNonStaticFields(new TreeSet<>(Comparator.comparing(Field::getName)), target, Class::getFields);
    }

    public static SortedSet<Field> getAllNonStaticFields(Class<?> target) {
        return getAllNonStaticFields(new TreeSet<>(Comparator.comparing(Field::getName)), target, Class::getDeclaredFields);
    }

    public static O<Field> getNonStaticDeclaredField(Class<?> target, String fieldName) {
        return O.of(getAllNonStaticFields(target).stream().filter($ -> $.getName().equals(fieldName)).findAny());
    }

    public static boolean isStatic(Field f) {
        return Modifier.isStatic(f.getModifiers());
    }

    private final static Map<Field, FieldAccessor> fieldAccessors = new ConcurrentHashMap<>();

    public static FieldAccessor accessor(Field f) {
        return fieldAccessors.computeIfAbsent(f, (field) -> {
            final O<F1<Object, Object>> getterMethod = getterMethod(field);
            final O<C2<Object, Object>> setterMethod = setterMethod(field);

            boolean isFinal = Modifier.isFinal(field.getModifiers());

            if (getterMethod.isEmpty() || (setterMethod.isEmpty() && !isFinal)) {
                try {
                    field.setAccessible(true);
                } catch (Exception exception) {
                    throw new RuntimeException("Failed making field '" + field.getDeclaringClass().getName() + "#"
                            + field.getName() + "' accessible; either change its visibility or write a custom "
                            + "TypeAdapter for its declaring type", exception);
                }
            }

            return new FieldAccessor(
                    getterMethod.orElse((object) -> Ex.toRuntime(() -> field.get(object))),
                    isFinal ? O.empty()
                            : setterMethod.or(() -> O.of((object, value) -> Ex.toRuntime(() -> field.set(object, value)))));
        });
    }

    public static F1<Object, Object> getter(Field f) {
        return accessor(f).getter();
    }

    public static O<C2<Object, Object>> setter(Field f) {
        return accessor(f).setter();
    }

    public static O<F1<Object, Object>> getterMethod(Field f) {
        final Class<?> declaringClass = f.getDeclaringClass();
        final Class<?> fieldType = f.getType();
        final String name = f.getName();
        String getterName = declaringClass.isRecord()
                            ? name
                            : fieldType == boolean.class
                              ? "is" + St.capFirst(name)
                              : "get" + St.capFirst(name);
        try {
            final Method method = declaringClass.getMethod(getterName);

            F1<Object, Object> getter = obj -> {
                try {
                    //todo if the class is not public we can't do it like this
                    return method.invoke(obj);
                } catch (Exception e) {
                    return Ex.thRow(e);
                }
            };
            return O.of(getter);
        } catch (NoSuchMethodException e) {
            return O.empty();
        }
    }

    public static O<C2<Object, Object>> setterMethod(Field f) {
        final Class<?> declaringClass = f.getDeclaringClass();
        final Class<?> fieldType = f.getType();
        final String name = f.getName();
        String setterName = "set" + St.capFirst(name);
        final O<Method> any = O.of(Arrays.stream(declaringClass.getMethods())
                .filter($ -> Fu.equal($.getName(), setterName) && $.getParameters().length == 1)
                .findAny());
        return any.map($ -> (object, setValue) -> {
            try {
                $.invoke(object, setValue);
            } catch (Exception e) {
                Ex.thRow(e);
            }
        });
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

    @SneakyThrows
    public static Class<?> cls4Name(String cls) {
        return Class.forName(cls);
    }
    //endregion
}
