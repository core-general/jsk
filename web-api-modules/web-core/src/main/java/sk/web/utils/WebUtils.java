package sk.web.utils;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
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


import sk.mvn.model.ApiDtoClassModel;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.St;
import sk.web.annotations.WebPath;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

import static sk.utils.statics.Cc.join;
import static sk.utils.statics.Cc.stream;
import static sk.utils.statics.St.endWith;

public final class WebUtils {
    private WebUtils() {throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");}

    public static String getMethodApiPath(O<String> basePath, Method m) {
        String rootPrefix = getBaseApiPath(basePath, m.getDeclaringClass());
        final O<WebPath> oWath = O.ofNull(m.getAnnotation(WebPath.class));

        String methodName = m.getName();
        O<String> realPath = oWath.map($ -> $.value());
        boolean appendMethodName = oWath.map(WebPath::appendMethodName).orElse(true);

        return getMethodApiPath(rootPrefix, methodName, realPath, appendMethodName);
    }

    public static String getBaseApiPath(O<String> basePath, Class<?> apiCls) {
        WebPath rootPath = apiCls.getAnnotation(WebPath.class);
        return basePath.map($ -> endWith($, "/")).orElse("") + (rootPath != null ? endWith(join("/", stream(rootPath.value())),
                "/") : "");
    }


    public static String getMethodApiPath(String rootPrefix, String methodName, O<String> realPath, boolean appendMethodName) {
        String methodPrefix = (
                rootPrefix
                        +
                        (realPath.isPresent()
                                ? appendMethodName
                                ? endWith(join("/", stream(realPath.get())), "/")
                                : join("/", stream(realPath.get()))
                                : "")
                        +
                        (appendMethodName ? methodName : "")
        ).replace("//", "/")
                .replace("http:/", "http://")
                .replace("https:/", "https://");

        return methodPrefix;
    }


    public static Method[] getActualApiMethods(Class<?> c) {
        return Cc.stream(c.getMethods()).filter($ -> !$.isDefault()).toArray(Method[]::new);
    }


    /*
    We want:
    int -> int
    Integer -> Integer
    SomeClass -> SomeClass
    a.b.c.SomeClass -> SomeClass
    a.b.c.SomeClass<a.b.c.SomeClass,a.b.c.SomeClass> -> SomeClass<SomeClass,SomeClass>
    a.b.c.SomeClass<a.b.c.SomeClass<a.b.c.SomeClass>> -> SomeClass<SomeClass<SomeClass>>
     */
    public static String simplify(Type t) {
        StringBuilder sb = new StringBuilder();
        if (t instanceof ParameterizedType) {
            final ParameterizedType parametrizedType = (ParameterizedType) t;
            sb.append(simpleTypeName(parametrizedType.getRawType().getTypeName()));
            final Type[] parametrizedArguments = parametrizedType.getActualTypeArguments();
            if (parametrizedArguments.length > 0) {
                sb.append("<");
                for (Type parametrizedArgument : parametrizedArguments) {
                    sb.append(simplify(parametrizedArgument)).append(",");
                }
                sb.replace(sb.length() - 1, sb.length(), "");
                sb.append(">");
            }
        } else {
            sb.append(simpleTypeName(t.getTypeName()));
        }

        return sb.toString();
    }

    public static ApiDtoClassModel emptyDtoTypeCreator(String name) {
        return new ApiDtoClassModel(name, false, Cc.l(), O.empty(), O.of("!CLASS NOT FOUND!"));
    }

    public static List<String> dtoTypeProcessorOrFilter(String type) {
        type = type.trim();
        if (type.contains(",") || type.contains("<")) {
            final List<String> collect = Cc.stream(type.split("[<,>]"))
                    .filter(St::isNotNullOrEmpty)
                    .map($ -> $.trim())
                    .flatMap($ -> dtoTypeProcessorOrFilter($).stream())
                    .collect(Collectors.toList());
            return collect;
        }

        if (isDtoStandardType(type)) { return Cc.lEmpty(); }

        return Cc.l(type);
    }

    private static boolean isDtoStandardType(String type) {
        switch (type) {
            case "byte":
            case "short":
            case "int":
            case "long":
            case "double":
            case "float":
            case "boolean":
            case "Byte":
            case "Short":
            case "Integer":
            case "Long":
            case "Double":
            case "Float":
            case "Boolean":
            case "ZonedDateTime":
            case "Instant":
            case "O":
            case "Optional":
            case "List":
            case "Set":
            case "Map":
            case "String":
            case "TreeMap":
            case "SortedMap":
            case "UUID":
            case "IdBase":
            case "IdUuid":
            case "IdString":
            case "byte[]":
                return true;
        }
        return false;
    }

    private static String simpleTypeName(String fullTypeName) {
        return St.subLL(fullTypeName.toString(), ".");
    }
}
