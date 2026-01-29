package sk.web.client;

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
import sk.services.bean.IServiceLocator;
import sk.services.except.IExcept;
import sk.services.http.IHttp;
import sk.services.http.IHttp.HttpBuilder;
import sk.services.http.model.CoreHttpResponse;
import sk.services.ids.IIds;
import sk.services.json.IJson;
import sk.services.retry.IRepeat;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.ids.IdBase;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.St;
import sk.utils.tuples.X;
import sk.utils.tuples.X1;
import sk.web.WebMethodType;
import sk.web.infogatherer.WebClassInfo;
import sk.web.infogatherer.WebClassInfoProvider;
import sk.web.infogatherer.WebMethodInfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
public class WebClientFactory {
    @Inject WebClassInfoProvider infoProvider;
    @Inject IHttp http;
    @Inject IJson json;
    @Inject IIds ids;
    @Inject IRepeat repeat;
    @Inject IExcept except;
    @Inject IServiceLocator beanProvider;

    // Platform abstractions for TeaVM compatibility
    @Inject WebPlatformSpecificHelper platformHelper;

    public <API, E> O<API> createWebApiClient(String basePath, Class<API> apiCls,
            WebClientInputHandler inputHandler, WebClientResultHandler<E> resultHandler) {
        return privateCreate(infoProvider.getClassModel(apiCls, O.ofNull(basePath)), apiCls, inputHandler, resultHandler);
    }

    public <API, E> O<API> createWebApiClient(String basePath, Class<API> apiCls, WebClientResultHandler<E> resultHandler) {
        return privateCreate(infoProvider.getClassModel(apiCls, O.ofNull(basePath)), apiCls, WebClientInputHandlerImpl.empty(),
                resultHandler);
    }

    private <API, E> O<API> privateCreate(WebClassInfo classInfo,
            Class<API> apiCls,
            WebClientInputHandler inputHandler,
            WebClientResultHandler<E> resultHandler) {
        if (!checkApiClassConformance(classInfo)) {
            return O.empty();
        }

        // Use WebPlatformSpecificHelper abstraction instead of Re.singleProxy
        API client = platformHelper.createClient(apiCls, classInfo, (methodName, args) -> {
            WebMethodInfo methodInfo = classInfo.getMethod(methodName);
            if (methodInfo == null) {
                // This shouldn't happen as WebPlatformSpecificHelper should handle Object methods
                return except.throwByDescription("Unknown method: " + methodName);
            }
            return executeApiCall(methodInfo, args, classInfo, inputHandler, resultHandler);
        });

        return O.of(client);
    }

    /**
     * Executes an API call using pre-extracted method metadata.
     * No runtime annotation reflection needed here.
     */
    private <E> Object executeApiCall(
            WebMethodInfo methodInfo,
            Object[] args,
            WebClassInfo classInfo,
            WebClientInputHandler inputHandler,
            WebClientResultHandler<E> resultHandler) {

        int retryCount = 1;
        int retrySleepMs = 0;

        WebApiClientExecutionModel request = constructExecute(args, classInfo, methodInfo);

        // Use pre-extracted idempotence config instead of annotation reflection
        if (methodInfo.getIdempotenceInfo().isPresent()) {
            WebMethodInfo.WebIdempotenceInfo idem = methodInfo.getIdempotenceInfo().get();
            retryCount = idem.getRetryCount();
            retrySleepMs = idem.getRetrySleepMs();
            final Map<String, String> whereToPut = idem.isParamOrHeader()
                                                   ? request.getParams()
                                                   : request.getHeaders();
            if (!whereToPut.containsKey(idem.getParamName())) {
                whereToPut.put(idem.getParamName(), ids.shortIdS());
            }
        }

        {
            WebApiClientExecutionModel __tempReq = request;
            request = inputHandler.preRequest().map($ -> $.apply(__tempReq)).orElse(__tempReq);
        }

        O<HttpBuilder<?>> oBuilder = prepareHttp(request);
        if (oBuilder.isEmpty()) {
            return except.throwByDescription("Can't prepare http request:" + request);
        }

        HttpBuilder<?> builder = oBuilder.get();
        builder.tryCount(retryCount);
        builder.trySleepMs(retrySleepMs);
        OneOf<CoreHttpResponse, Exception> resp = builder.goResponse();
        if (resp.isLeft()) {
            CoreHttpResponse left = resp.left();
            final WebRequestResultModel<?> resultModel = new WebRequestResultModel<>(request, left,
                    methodInfo.getReturnValue().getType());
            OneOf<?, E> tOneOf = resultHandler.processResult(resultModel);
            return tOneOf.isRight() ? resultHandler.doInCaseOfProblem(resultModel, tOneOf.right()) : tOneOf.left();
        } else {
            return Ex.thRow(resp.right());
        }
    }

    /**
     * Checks API class conformance using pre-extracted WebClassInfo.
     * No runtime reflection needed.
     */
    private boolean checkApiClassConformance(WebClassInfo classInfo) {
        //check client auth providers existence
        classInfo.getMethods().forEach(methodInfo -> {
            methodInfo.getAuthInfo().ifPresent(authInfo -> {
                if (beanProvider.getService(authInfo.getClientProvider()).isEmpty()) {
                    except.throwByDescription("PROBLEM no bean found:" + authInfo.getClientProvider());
                }
            });
        });

        return true;
    }

    private O<HttpBuilder<?>> prepareHttp(WebApiClientExecutionModel request) {
        if (request.getMethod() == WebMethodType.GET) {
            // Use WebUrlEncoder abstraction instead of URLEncoder
            String encodedURL = request.getParams().entrySet().stream()
                    .map(kv -> kv.getKey() + "=" + platformHelper.urlEncode(kv.getValue()))
                    .collect(Collectors.joining("&", request.getFullUrl() + "?", ""));

            IHttp.HttpGetBuilder builder = http.get(encodedURL);

            builder.headers().putAll(request.getHeaders());
            if (request.getRaw().size() > 0) {
                return O.empty();
            }

            return O.of(builder);
        } else if ((request.getMethod() == WebMethodType.POST_MULTI || request.getMethod() == WebMethodType.POST_MULTI_SURE)
                   && (request.getParams().size() > 0 || request.getRaw().size() > 0)) {
            IHttp.HttpMultipartBuilder builder = http.postMulti(request.getFullUrl());
            builder.headers().putAll(request.getHeaders());
            builder.parameters().putAll(request.getParams());
            builder.rawParameters().putAll(request.getRaw());
            return O.of(builder);
        } else if (request.getMethod() == WebMethodType.POST_BODY) {
            IHttp.HttpBodyBuilder builder = http.postBody(request.getFullUrl());
            builder.headers().putAll(request.getHeaders());
            if (request.getParams().size() == 1) {
                builder.body(request.getParams().entrySet().iterator().next().getValue());
            } else if (request.getRaw().size() == 1) {
                builder.body(request.getRaw().entrySet().iterator().next().getValue());
            } else {
                return O.empty();
            }
            return O.of(builder);
        } else /*if (request.getMethod() == WebMethodType.POST_FORM)*/ {
            IHttp.HttpFormBuilder builder = http.postForm(request.getFullUrl());
            builder.headers().putAll(request.getHeaders());
            builder.parameters().putAll(request.getParams());
            if (request.getRaw().size() > 0) {
                return O.empty();
            }
            return O.of(builder);
        }
    }

    /**
     * Constructs execution model using pre-extracted WebMethodInfo.
     * Uses pre-extracted auth config instead of WebApiMethod.
     */
    private WebApiClientExecutionModel constructExecute(Object[] a, WebClassInfo methods,
            WebMethodInfo methodInfo) {
        Map<String, String> h = Cc.m();
        Map<String, String> p = Cc.m();
        Map<String, byte[]> raw = Cc.m();

        List<WebMethodInfo.ParameterNameAndType> paramAndTypes = methodInfo.getParamAndTypes();
        List<String> names = paramAndTypes.stream().map($ -> $.getName()).collect(Collectors.toList());
        if ((a == null && names.size() != 0) || (a != null && names.size() != a.length)) {
            return except.throwByDescription("names.size()!=methodArg.length:" + names.size() + " " + a.length);
        } else {
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                Object obj = a[i];
                WebMethodInfo.ParameterNameAndType paramInfo = paramAndTypes.get(i);
                putParam(obj, paramInfo, name, p, raw);
            }
        }

        // Use pre-extracted auth config instead of WebApiMethod
        methodInfo.getAuthInfo().ifPresent(auth -> {
            O.ofNull(auth.getPassword())
                    .filter($ -> !St.isNullOrEmpty($))
                    .or(() -> beanProvider.getService(auth.getClientProvider()).flatMap($ -> $.getSecret4Client()))
                    .ifPresent($ -> (auth.isParamOrHeader() ? p : h).put(auth.getParamName(), $));
        });

        return new WebApiClientExecutionModel(
                ids.shortId(),
                tunePathParams(methodInfo.getFullMethodPath(), p),
                methodInfo.getType(), h, p,
                raw);
    }

    String tunePathParams(String path, Map<String, String> p) {
        final StringBuilder finalPath = new StringBuilder();

        if (path.startsWith("https:") || path.startsWith("http:")) {
            try {
                final URL url = new URL(path);
                final String contextPath = tunePathParams(url.getFile(), p);
                final URL finalUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), contextPath);
                finalPath.append(finalUrl.toString());
            } catch (MalformedURLException e) {
                return Ex.thRow(e);
            }
        } else {
            X1<StringBuilder> currentParameter = X.x(null);
            St.forEachChar(path, _char -> {
                if (_char == ':') {
                    if (currentParameter.get() != null) {
                        finalPath.append(p.getOrDefault(currentParameter.get().toString(), currentParameter.get().toString()));
                    }
                    currentParameter.set(new StringBuilder(""));
                } else {
                    if (currentParameter.get() != null) {
                        if (Character.isLetterOrDigit(_char) || '_' == _char) {
                            currentParameter.get().append(_char);
                        } else {
                            finalPath
                                    .append(p.getOrDefault(currentParameter.get().toString(), currentParameter.get().toString()));
                            finalPath.append(_char);
                            currentParameter.set(null);
                        }
                    } else {
                        finalPath.append(_char);
                    }
                }
            });
            if (currentParameter.get() != null) {
                finalPath.append(p.getOrDefault(currentParameter.get().toString(), currentParameter.get().toString()));
                currentParameter.set(null);
            }
        }


        return finalPath.toString();
    }

    private void putParam(Object value, WebMethodInfo.ParameterNameAndType paramInfo, String name, Map<String, String> p,
            Map<String, byte[]> raw) {
        if (value == null) {
            return;
        }
        final Class cls = value.getClass();
        if (value instanceof Optional) {
            if (((Optional<?>) value).isPresent()) {
                putParam(((Optional<?>) value).get(), paramInfo, name, p, raw);
            }
        } else if (value instanceof O) {
            if (((O<?>) value).isPresent()) {
                putParam(((O<?>) value).get(), paramInfo, name, p, raw);
            }
        } else if (value instanceof byte[]) {
            raw.put(name, (byte[]) value);
        } else if (cls.isPrimitive()
                   || cls == UUID.class || cls == String.class
                   || cls == Integer.class || cls == Long.class || cls == Float.class || cls == Double.class
        ) {
            p.put(name, Objects.toString(value));
        } else if (cls.isEnum()) {
            p.put(name, ((Enum) value).name());
        } else if (value instanceof IdBase) {
            p.put(name, value.toString());
        } else {
            String jsoned = json.to(value);
            if (jsoned.length() > 1 && jsoned.startsWith("\"") && jsoned.endsWith("\"")) {
                //if it's json string, then we need to remove surrounding "
                jsoned = jsoned.substring(1, jsoned.length() - 1);
            }
            p.put(name, jsoned);
        }
    }

}
