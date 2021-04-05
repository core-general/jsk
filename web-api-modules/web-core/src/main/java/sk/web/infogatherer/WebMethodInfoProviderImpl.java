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
import sk.web.annotations.type.WebGET;
import sk.web.annotations.type.WebMethod;
import sk.web.annotations.type.WebPOST;

import javax.inject.Inject;
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
                        "Please enable plugin web-api-info-generator-maven-plugin for:" + apiCls));

        validateModel(currentMethods, precompiledModel)
                .ifPresent($ -> except.throwByDescription("Can't parse api for " + apiCls + "\n" + $));


        Map<String, WebMethodInfo> actualMethodInfos = currentMethods.entrySet().stream()
                .map($ -> {
                    WebMethodType method = O.ofNull($.getValue().getAnnotation(WebGET.class))
                            .map(__ -> WebMethodType.GET)
                            .or(() -> O.ofNull($.getValue().getAnnotation(WebPOST.class))
                                    .map(x -> x.forceMultipart() ? WebMethodType.POST_MULTI_SURE : WebMethodType.POST_MULTI))
                            .or(() -> O.ofNull($.getValue().getAnnotation(WebMethod.class)).map(WebMethod::method))
                            .orElse(WebMethodType.POST_MULTI);
                    ApiMethodModel methodModel = precompiledModel.getEndpoints().get($.getKey());

                    Type[] paramTypes = $.getValue().getGenericParameterTypes();

                    return new WebMethodInfo($.getValue(),
                            getMethodApiPath(basePath, $.getValue()),
                            method,
                            new WebMethodInfo.ParameterNameAndType(null, TypeWrap.raw($.getValue().getReturnType())),
                            mapEachWithIndex(Arrays.asList($.getValue().getParameters()),
                                    (p, i) -> new WebMethodInfo.ParameterNameAndType(
                                            methodModel.getParams().get(i).getName(),
                                            TypeWrap.raw(paramTypes[i]))),
                            methodModel);
                })
                .collect(toM($ -> $.getMethod().getName(), $ -> $));

        return new WebClassInfo(
                startWith(endWith(getBaseApiPath(basePath, apiCls), "/"), "/"),
                precompiledModel.getApiClass(),
                precompiledModel.getComment(),
                actualMethodInfos,
                precompiledModel.getClasses());
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
