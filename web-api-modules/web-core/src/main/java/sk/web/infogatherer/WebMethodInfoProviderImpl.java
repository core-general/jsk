package sk.web.infogatherer;

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

import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import sk.mvn.ApiClassUtil;
import sk.mvn.model.ApiClassModel;
import sk.mvn.model.ApiMethodModel;
import sk.services.except.IExcept;
import sk.utils.functional.O;
import sk.utils.javafixes.TypeWrap;
import sk.utils.tuples.X;
import sk.web.WebMethodType;
import sk.web.annotations.WebAuth;
import sk.web.annotations.WebAuthNO;
import sk.web.annotations.WebIdempotence;
import sk.web.annotations.WebIdempotenceNO;
import sk.web.annotations.type.WebGET;
import sk.web.annotations.type.WebMethod;
import sk.web.annotations.type.WebPOST;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static sk.utils.statics.Cc.*;
import static sk.utils.statics.St.endWith;
import static sk.utils.statics.St.startWith;
import static sk.web.utils.WebUtils.*;

@AllArgsConstructor
@NoArgsConstructor
public class WebMethodInfoProviderImpl implements WebClassInfoProvider {
    @Inject IExcept except;
    @Inject ApiClassUtil hasher;

    public <API> WebClassInfo getClassModel(Class<API> apiCls, O<String> basePath) {
        Map<String, Method> currentMethods = stream(getActualApiMethods(apiCls)).collect(toM($ -> $.getName(), $ -> $));
        ApiClassModel precompiledModel = hasher.getApiClassFromResources(apiCls)
                .orElseGet(() -> except.throwByDescription("Api class descriptor file not found \n" +
                                                           "Please enable plugin web-api-info-generator-maven-plugin for:" +
                                                           apiCls));

        validateModel(currentMethods, precompiledModel)
                .ifPresent($ -> except.throwByDescription("Can't parse api for " + apiCls + "\n" + $));


        Map<String, WebMethodInfo> actualMethodInfos = currentMethods.entrySet().stream()
                .map($ -> {
                    Method m = $.getValue();
                    WebMethodType method = O.ofNull(m.getAnnotation(WebGET.class))
                            .map(__ -> WebMethodType.GET)
                            .or(() -> O.ofNull(m.getAnnotation(WebPOST.class))
                                    .map(x -> x.forceMultipart() ? WebMethodType.POST_MULTI_SURE : WebMethodType.POST_MULTI))
                            .or(() -> O.ofNull(m.getAnnotation(WebMethod.class)).map(WebMethod::method))
                            .orElse(WebMethodType.POST_MULTI);
                    ApiMethodModel methodModel = precompiledModel.getEndpoints().get($.getKey());

                    Type[] paramTypes = m.getGenericParameterTypes();

                    // Extract auth annotation (method level overrides class level, WebAuthNO disables)
                    O<WebMethodInfo.WebAuthInfo> authInfo = resolveAnnotation(m, apiCls, WebAuth.class, WebAuthNO.class)
                            .map(a -> new WebMethodInfo.WebAuthInfo(
                                    a.paramName(),
                                    a.isParamOrHeader(),
                                    a.getPassword(),
                                    a.srvProvider(),
                                    a.clientProvider()));

                    // Extract idempotence annotation (method level overrides class level, WebIdempotenceNO disables)
                    O<WebMethodInfo.WebIdempotenceInfo> idempotenceInfo =
                            resolveAnnotation(m, apiCls, WebIdempotence.class, WebIdempotenceNO.class)
                                    .map(a -> new WebMethodInfo.WebIdempotenceInfo(
                                            a.paramName(),
                                            a.isParamOrHeader(),
                                            a.force(),
                                            a.retryCount(),
                                            a.retrySleepMs()));

                    return new WebMethodInfo(m,
                            getMethodApiPath(basePath, m),
                            method,
                            new WebMethodInfo.ParameterNameAndType(null, TypeWrap.raw(m.getGenericReturnType()), false),
                            mapEachWithIndex(Arrays.asList(m.getParameters()),
                                    (p, i) -> new WebMethodInfo.ParameterNameAndType(
                                            methodModel.getParams().get(i).getName(),
                                            TypeWrap.raw(paramTypes[i]),
                                            methodModel.getParams().get(i).isMerging()
                                    )),
                            methodModel,
                            authInfo,
                            idempotenceInfo);
                })
                .collect(toM($ -> $.getMethod().getName(), $ -> $));

        return new WebClassInfo(
                startWith(endWith(getBaseApiPath(basePath, apiCls), "/"), "/"),
                precompiledModel.getApiClass(),
                precompiledModel.getComment(),
                actualMethodInfos,
                precompiledModel.getClasses());
    }

    /**
     * Resolves an annotation considering method-level, class-level, and opposite annotation.
     * Method-level annotation takes precedence. If opposite annotation is present on method, returns empty.
     */
    private <A extends Annotation, N extends Annotation> O<A> resolveAnnotation(
            Method method, Class<?> cls, Class<A> annotationClass, Class<N> oppositeClass) {
        // Check if method has the opposite annotation (which disables the feature)
        if (method.getAnnotation(oppositeClass) != null) {
            return O.empty();
        }
        // Method-level annotation takes precedence
        A methodAnnotation = method.getAnnotation(annotationClass);
        if (methodAnnotation != null) {
            return O.of(methodAnnotation);
        }
        // Fall back to class-level annotation
        return O.ofNull(cls.getAnnotation(annotationClass));
    }

    private O<String> validateModel(Map<String, Method> methods, ApiClassModel model) {
        Map<String, String> currentMethodHashes = methods.entrySet().stream()
                .map($ -> X.x($.getKey(), hasher.calculateHashCode(
                        getMethodApiPath(O.empty(), $.getValue()),
                        simplify($.getValue().getGenericReturnType()),
                        stream($.getValue().getGenericParameterTypes()).map($$ -> simplify($$)).collect(toL())
                )))
                .collect(toMX2());

        Set<String> thisMethods =
                currentMethodHashes.entrySet().stream().map($ -> $.getKey() + ":" + $.getValue()).collect(toSet());

        Set<String> otherMethods = model.getEndpoints().entrySet().stream()
                .map($ -> $.getKey() + ":" + $.getValue().getNameAndParamHash())
                .collect(toSet());

        Set<String> thisMethodsCopy = new HashSet<>(thisMethods);
        thisMethods.removeAll(otherMethods);
        otherMethods.removeAll(thisMethodsCopy);

        if (thisMethods.size() == 0 && otherMethods.size() == 0) {

            return O.empty();
        } else {
            String neuToOld = "Methods in new API class not existing in old:" +
                              thisMethods.stream().sorted().collect(Collectors.joining(", "));
            String oldToNeu = "Methods in old API class not existing in new:" +
                              otherMethods.stream().sorted().collect(Collectors.joining(", "));

            return O.of("Api problems: \n" + neuToOld + "\n" + oldToNeu);
        }
    }
}
