package sk.web.server;

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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import sk.exceptions.JskProblem;
import sk.services.bean.IServiceProvider;
import sk.services.except.IExcept;
import sk.services.ids.IIds;
import sk.services.json.IJson;
import sk.services.nodeinfo.IBeanInfoSubscriber;
import sk.services.shutdown.AppStopListener;
import sk.services.time.ITime;
import sk.utils.functional.*;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Cc;
import sk.utils.statics.Re;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;
import sk.web.WebMethodType;
import sk.web.annotations.*;
import sk.web.annotations.type.WebPOST;
import sk.web.exceptions.IWebExcept;
import sk.web.infogatherer.WebClassInfo;
import sk.web.infogatherer.WebClassInfoProvider;
import sk.web.infogatherer.WebMethodInfo;
import sk.web.renders.WebFilterOutput;
import sk.web.renders.WebRenderEmptyProvider;
import sk.web.renders.WebRenderResult;
import sk.web.server.context.*;
import sk.web.server.filters.WebServerFilter;
import sk.web.server.filters.WebServerFilterContext;
import sk.web.server.filters.WebServerFilterNext;
import sk.web.server.filters.standard.*;
import sk.web.server.model.WebProblemWithRequestBodyException;
import sk.web.server.params.WebExceptionParams;
import sk.web.utils.WebApiMethod;
import sk.web.utils.WebUtils;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static sk.utils.functional.O.*;
import static sk.utils.statics.St.*;
import static sk.web.renders.WebRenderType.JSON;

@Log4j2
public class WebServerCore<API>
        implements IBeanInfoSubscriber<WebServerCore.WebServerShortInfo>, AppStopListener {

    public static final String INTERNAL_ERROR = "internal_error";
    public static final String IO_PROBLEM_WHILE_READ_BODY = "io_problem_while_read_body";

    @Inject protected WebAuthFilter authFilter;
    @Inject protected WebDdosFilter ddosFilter;
    @Inject protected WebDefaultHeadersFilter defaultHeadersFilter;
    @Inject protected WebIdempotenceFilter idempotenceFilter;
    @Inject protected WebRequestLoggingFilter requestLoggingFilter;
    @Inject protected WebShutdownFilter shutdownFilter;
    @Inject protected WebExceptionFilter exceptionFilter;
    @Inject protected WebRenderFilter webRenderFilter;
    @Inject protected WebContextExplicatorFilter contextFilter;

    @Inject protected WebClassInfoProvider infoProvider;

    @Inject protected WebExceptionParams exceptConf;
    @Inject protected IServiceProvider beanProvider;
    @Inject protected IWebExcept webExcept;
    @Inject protected IExcept except;
    @Inject protected IJson json;
    @Inject protected ITime time;
    @Inject protected IIds ids;

    @Getter
    @Setter
    private volatile boolean shouldStop;
    private volatile boolean created;

    private final F2<String, Boolean, Supplier<Object>> NON_NULL_CHECK = (paramName, nullAllowed) -> () ->
            nullAllowed
                    ? null
                    : webExcept.throwMissingParameter(paramName, true);

    private final WebServerInfo info;

    @Getter private final Class<API> apiClass;
    @Getter private final O<String> basePath;
    private final API serverApiImpl;

    public WebServerCore(Class<API> apiClass, API impl) {
        this(apiClass, impl, null);
    }

    public WebServerCore(Class<API> apiClass, API serverApiImpl, String basePath) {
        this.apiClass = apiClass;
        this.serverApiImpl = serverApiImpl;
        this.basePath = ofNull(basePath);
        this.info = new WebServerInfo(getApiClass().getSimpleName());
    }

    protected O<List<WebServerFilter>>
    getAdditionalFilters(O<Method> methodOrAll) {
        return empty();
    }

    protected O<F1<Class<? extends Exception>, O<F2<Exception, WebRequestInnerContext, WebFilterOutput>>>>
    getExceptionProcessors(O<Method> methodOrAll) {
        return empty();
    }

    protected List<WebServerFilter>
    getDefaultFilters() {
        return Cc.l(
                requestLoggingFilter,
                defaultHeadersFilter,
                ddosFilter,
                idempotenceFilter,
                exceptionFilter,
                shutdownFilter,
                authFilter,
                webRenderFilter,
                contextFilter
                /*-->ACTUAL PAYLOAD PROCESSOR IS HERE RIGHT AFTER CONTEXT FILTER*/
        );
    }

    protected void
    addAuxiliaryMethods(Class<API> apiClass, WebServerContextWithInfo env) {/*nothing in the base class*/}

    protected O<? extends sk.web.renders.WebRender>
    getDefaultRender() {
        return JSON.getRender(beanProvider);
    }

    public synchronized final void create(WebServerContext _env) {
        if (created) {
            return;
        }
        WebServerContextWithInfo env = new WebServerContextWithInfo(_env);

        final Class<API> apiClass = getApiClass();
        final Method[] methods = WebUtils.getActualApiMethods(apiClass);
        final WebClassInfo methodModel = infoProvider.getClassModel(apiClass, getBasePath());

        if (!checkApiClassConformance(apiClass)) {
            except.throwByDescription("Class conformance failed for:" + apiClass);
        }

        final List<WebServerFilter> addFiltersForAllMethods = getAdditionalFilters(empty()).orElse(Cc.lEmpty());
        final O<F1<Class<? extends Exception>, O<F2<Exception, WebRequestInnerContext, WebFilterOutput>>>> eProc =
                getExceptionProcessors(empty());

        final List<WebServerFilter> defaultFilters = getDefaultFilters();
        StringBuilder toLog = new StringBuilder(getNonMethodSpecificInfo(apiClass, defaultFilters));

        addAuxiliaryMethods(apiClass, env);

        for (Method method : methods) {
            final WebApiMethod<API> webApiMethod = new WebApiMethod<>(apiClass, of(method), false);

            final WebMethodInfo methodInfo = methodModel.getMethod(method.getName());
            final boolean mustMultipart = mustMultipart(methodInfo, webApiMethod);

            final ArrayList<WebServerFilter> methodSpecificFilters =
                    new ArrayList<>(getAdditionalFilters(of(method)).orElse(addFiltersForAllMethods));
            toLog.append(getMethodSpecificInfo(methodSpecificFilters, methodInfo));
            final TreeSet<WebServerFilter> methodFilters = validateFilters(Cc.addAll(methodSpecificFilters, defaultFilters));

            final O<F1<Class<? extends Exception>, O<F2<Exception, WebRequestInnerContext, WebFilterOutput>>>> eCur =
                    getExceptionProcessors(of(method));
            final F2<String, WebRequestReadableOuterContext, ?> paramGetters =
                    createParamGetters(methodInfo.getParamAndTypes(), methodInfo.getType() == WebMethodType.POST_BODY);

            final sk.web.renders.WebRender foundRender = webApiMethod.getAnnotation(WebRender.class)
                    .flatMap($ -> $.getProvider() == WebRenderEmptyProvider.class
                            ? $.value().getRender(beanProvider)
                            : Re.createObjectByDefault($.getProvider()).flatMap(x -> x.getRender(beanProvider)))
                    .or(() -> getDefaultRender())
                    .orElseGet(() -> except.throwByDescription("Can't find default render and JSON render is not found"));

            final O<WebAuth> webAuth = webApiMethod.getAnnotation(WebAuth.class, WebAuthNO.class);
            final O<WebIdempotence> webIdempotence = webApiMethod.getAnnotation(WebIdempotence.class, WebIdempotenceNO.class);

            final C1<WebRequestOuterFullContext> outerProcessor = outerContext -> {
                final WebRequestInnerContext innerContext = prepareInnerContext(outerContext, webApiMethod, methodInfo.getType(),
                        eCur.or(() -> eProc), webAuth, webIdempotence, foundRender);
                try {
                    WebServerFilterNext curent =
                            invokeBaseMethodSupplier(method, paramGetters, methodInfo, mustMultipart, outerContext);
                    curent = createFilterChain(webApiMethod, curent, innerContext, methodFilters);
                    final WebRenderResult renderResult = curent.invokeNext().render(foundRender, webExcept);
                    outerContext.setResponse(renderResult);
                } catch (WebProblemWithRequestBodyException exc) {
                    //should be fixed, that's why stacktrace
                    log.warn("WebProblemWithRequestBodyException");
                    outerContext.setError(503,
                            webExcept.getDefaultExceptionRender(), JskProblem.code(IO_PROBLEM_WHILE_READ_BODY));
                } catch (Exception unknownExc) {
                    //should be fixed, that's why stacktrace
                    log.error("Error on request unknown: " + exceptConf.getUnknownExceptionHttpCode() + " " +
                            INTERNAL_ERROR, unknownExc);
                    outerContext.setError(exceptConf.getUnknownExceptionHttpCode(),
                            webExcept.getDefaultExceptionRender(), JskProblem.code(INTERNAL_ERROR));
                }
            };

            switch (methodInfo.getType()) {
                case POST_MULTI_SURE:
                    env.addPost(methodInfo.getFullMethodPath(), outerProcessor, true, of(methodInfo));
                    break;
                case POST_MULTI:
                case POST_FORM:
                case POST_BODY:
                    env.addPost(methodInfo.getFullMethodPath(), outerProcessor, false, of(methodInfo));
                    break;
                case GET:
                    env.addGet(methodInfo.getFullMethodPath(), outerProcessor, of(methodInfo));
                    break;
            }
        }

        log.info(toLog.toString());

        created = true;
    }

    protected String getMethodSpecificInfo(ArrayList<WebServerFilter> methodSpecificFilters, WebMethodInfo methodInfo) {
        StringBuilder sb = new StringBuilder("");
        sb.append(addTabsLeft(methodInfo.getType() + ":" + methodInfo.getFullMethodPath(), 1)).append("\n");
        if (methodSpecificFilters.size() > 0) {
            sb.append(addTabsLeft("Additional filters: " + methodInfo.getFullMethodPath(), 2)).append("\n");
            sb.append(addTabsLeft(new TreeSet<>(methodSpecificFilters).stream()
                    .map($ -> $.getClass().getTypeName() + ":" + $.getFilterPriority())
                    .collect(Collectors.joining("\n")), 3)).append("\n");
        }
        return sb.toString();
    }

    protected String getNonMethodSpecificInfo(Class<API> apiClass, List<WebServerFilter> addFiltersForAllMethods) {
        StringBuilder sb = new StringBuilder("\n");
        sb.append("API: " + apiClass.getName()).append("\n");
        sb.append(addTabsLeft("Default filters: ", 1)).append("\n");
        sb.append(addTabsLeft(new TreeSet<>(addFiltersForAllMethods).stream()
                .map($ -> $.getClass().getSimpleName() + ":" + $.getFilterPriority())
                .collect(Collectors.joining("\n")), 2)).append("\n").append("\n");
        return sb.toString();
    }

    protected final TreeSet<WebServerFilter> validateFilters(List<WebServerFilter> allFilters) {
        final String error = allFilters.stream().collect(groupingBy($ -> $.getFilterPriority()))
                .entrySet().stream()
                .filter($ -> $.getValue().size() > 1)
                .sorted(Comparator.comparing($ -> $.getKey()))
                .map($ -> "Filters with same priority " + $.getKey() + " : " + Cc.join($.getValue(), x -> x.getClass().getName()))
                .collect(joining("\n"));
        if (!isNullOrEmpty(error)) {
            return except.throwByDescription("Bad filter configuration:\n" + error);
        }
        return new TreeSet<>(allFilters);
    }

    protected final WebServerFilterNext createFilterChain(WebApiMethod<API> api, WebServerFilterNext realPayload,
            WebRequestInnerContext innerContext,
            NavigableSet<WebServerFilter> methodFilters) {
        for (WebServerFilter filter : methodFilters.descendingSet()) {
            WebServerFilterNext finalCurent = realPayload;
            realPayload = () -> filter.invoke(new WebServerFilterContext<>(api, innerContext, finalCurent));
        }
        return realPayload;
    }

    protected final WebRequestInnerContext prepareInnerContext(WebRequestOuterFullContext outerContext,
            WebApiMethod<API> apiMethod, WebMethodType webMethodType,
            O<F1<Class<? extends Exception>, O<F2<Exception, WebRequestInnerContext, WebFilterOutput>>>> exceptions,
            O<WebAuth> webAuth, O<WebIdempotence> webIdempotence, sk.web.renders.WebRender foundRender) {
        return new WebRequestInnerContextImpl(
                shouldStop,
                ids.shortIdS(),
                time.nowZ(),
                time.nowNano4Dif(),
                getIpInfo(outerContext),
                apiMethod,
                webMethodType,
                exceptions,
                webAuth,
                webIdempotence,
                foundRender,
                outerContext
        );
    }

    private WebRequestIp getIpInfo(WebRequestOuterFullContext outerContext) {
        final String ip = outerContext.getIp();
        final O<String> proxy1 = outerContext.getRequestHeader("X-Forwarded-For");
        final O<String> proxy2 = outerContext.getRequestHeader("X-Forwarded-For-1");

        List<String> proxies = Cc.l();
        proxy2.ifPresentOrElse(w -> {
            proxy1.ifPresent(proxies::add);
            proxies.add(ip);
        }, () -> {
            proxy1.ifPresent((x) -> proxies.add(ip));
        });

        return new WebRequestIp(proxy2.or(() -> proxy1).orElse(ip), proxies);
    }

    private boolean mustMultipart(WebMethodInfo methodInfo, WebApiMethod<API> apiMethod) {
        O<WebPOST> annotation = apiMethod.getAnnotation(WebPOST.class);
        return (annotation.isPresent() && annotation.get().forceMultipart());
    }

    private F2<String, WebRequestReadableOuterContext, ?> createParamGetters(List<WebMethodInfo.ParameterNameAndType> params,
            boolean onlyBody) {
        Map<String, F1<WebRequestReadableOuterContext, ?>> cachedInvokers = Cc.m();
        if (params.size() != 1 && onlyBody) {
            return except.throwByDescription("Not one parameter, but should be only one for body");
        }
        for (int i = 0; i < params.size(); i++) {
            WebMethodInfo.ParameterNameAndType nt = params.get(i);
            F1<WebRequestReadableOuterContext, Object> oneParameter =
                    getOneParameter(nt.getName(), nt.getType().getType(), false, onlyBody);
            cachedInvokers.put(nt.getName(), oneParameter);
        }
        return (param, webRequestContext) -> {
            //try {
            return cachedInvokers.get(param).apply(webRequestContext);
            //} catch (Exception e) {
            //    return Ex.thRow("Can't process parameter: " + param + " all parameters: " + Cc.join(cachedInvokers.keySet()),
            //    e);
            //}
        };
    }

    private F1<WebRequestReadableOuterContext, Object> getOneParameter(String paramName, Type type, boolean nullAllowed,
            boolean onlyBody) {
        if (type instanceof ParameterizedType) {
            Type[] typePar = ((ParameterizedType) type).getActualTypeArguments();
            if (((ParameterizedType) type).getRawType() == O.class) {
                return getOneParameter(paramName, typePar.length > 0 ? typePar[0] : Object.class, true, onlyBody)
                        .andThen(O::ofNullable);
            } else if (((ParameterizedType) type).getRawType() == Optional.class) {
                return getOneParameter(paramName, typePar.length > 0 ? typePar[0] : Object.class, true, onlyBody)
                        .andThen(Optional::ofNullable);
            } else {
                return fromString(paramName, nullAllowed, s -> json.from(s, TypeWrap.raw(type)), onlyBody);
            }
        } else if (type == String.class) {
            return fromString(paramName, nullAllowed, s -> s, onlyBody);
        } else if (type == boolean.class) {
            return fromString(paramName, nullAllowed, s -> Boolean.parseBoolean(s.toLowerCase()), onlyBody);
        } else if (type == int.class) {
            return fromString(paramName, nullAllowed, Integer::parseInt, onlyBody);
        } else if (type == float.class) {
            return fromString(paramName, nullAllowed, Float::parseFloat, onlyBody);
        } else if (type == double.class) {
            return fromString(paramName, nullAllowed, Double::parseDouble, onlyBody);
        } else if (type == long.class) {
            return fromString(paramName, nullAllowed, Long::parseLong, onlyBody);
        } else if (type == Boolean.class) {
            return fromString(paramName, nullAllowed, s -> Boolean.parseBoolean(s.toLowerCase()), onlyBody);
        } else if (type == Integer.class) {
            return fromString(paramName, nullAllowed, Integer::parseInt, onlyBody);
        } else if (type == Float.class) {
            return fromString(paramName, nullAllowed, Float::parseFloat, onlyBody);
        } else if (type == Double.class) {
            return fromString(paramName, nullAllowed, Double::parseDouble, onlyBody);
        } else if (type == Long.class) {
            return fromString(paramName, nullAllowed, Long::parseLong, onlyBody);
        } else if (type == UUID.class) {
            return fromString(paramName, nullAllowed, UUID::fromString, onlyBody);
        } else if (type instanceof Class && ((Class<?>) type).isEnum()) {
            return fromString(paramName, nullAllowed, s -> Re.findInEnum((Class<? extends Enum>) type, s).get(), onlyBody);
        } else if (type == byte[].class) {
            return w -> (onlyBody ? w.getBody() : w.getParamAsBytes(paramName))
                    .orElseGet(() -> (byte[]) NON_NULL_CHECK.apply(paramName, nullAllowed).get());
        } else {
            return fromString(paramName, nullAllowed, s -> json.from(s, TypeWrap.raw(type)), onlyBody);
        }
    }

    private F1<WebRequestReadableOuterContext, Object>
    fromString(String paramName, boolean nullAllowed, F1<String, Object> transform, boolean isBody) {
        return w -> (isBody ? w.getBody().map($ -> bytesToS($)) : w.getParamAsString(paramName))
                .map((t) -> {
                    try {
                        return transform.apply(t);
                    } catch (Exception e) {
                        return webExcept.throwBySubstatus(400, "coerce_error",
                                "Wrong value:'" + t + "' for parameter:'" + paramName + "'");
                    }
                })
                .orElseGet(NON_NULL_CHECK.apply(paramName, nullAllowed));
    }

    private WebServerFilterNext invokeBaseMethodSupplier(Method method, F2<String, WebRequestReadableOuterContext, ?> paramGetter,
            WebMethodInfo methodInfo, boolean mustMultipart, WebRequestReadableOuterContext context) {
        return () -> {
            if (mustMultipart && !context.isMultipart()) {
                return webExcept.throwBySubstatus(400, "request_must_be_multipart", "Must be multipart");
            }
            Object[] params = methodInfo.getParamAndTypes().stream().map($ -> paramGetter.apply($.getName(), context)).toArray();
            try {
                if (methodInfo.getReturnValue().getType().getType() == void.class) {
                    method.invoke(serverApiImpl, params);
                    return WebFilterOutput.empty();
                } else {
                    return WebFilterOutput.rawValue(200, method.invoke(serverApiImpl, params));
                }
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof RuntimeException) {
                    throw (RuntimeException) e.getTargetException();
                } else {
                    throw new RuntimeException("Problem with method: " + methodInfo.getFullMethodPath(), e);
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new RuntimeException("Problem with method: " + methodInfo.getFullMethodPath(), e);
            }
        };
    }

    private <API> boolean checkApiClassConformance(Class<API> apiCls) {
        //check server auth providers existence
        Cc.stream(WebUtils.getActualApiMethods(apiCls)).forEach($ -> {
            WebAuth annotation = $.getAnnotation(WebAuth.class);
            if (annotation != null && beanProvider.getService(annotation.srvProvider()).isEmpty()) {
                except.throwByDescription("PROBLEM no bean found:" + annotation.srvProvider());
            }
        });

        return true;
    }

    @Override
    public X2<String, WebServerShortInfo> gatherDiagnosticInfo() {
        return X.x("WEB_" + serverApiImpl.getClass().getName(), info.toShortInfo());
    }

    @Override
    public long waitBeforeStopMs() {
        return 100;
    }

    @Override
    public void onStop() {
        setShouldStop(true);
    }

    protected class WebServerContextWithInfo {
        private final WebServerContext _env;

        public WebServerContextWithInfo(WebServerContext _env) {this._env = _env;}

        public void addPost(String path, C1<WebRequestOuterFullContext> webRequestProcessor, boolean multipartSure,
                O<WebMethodInfo> methodInfo) {
            methodInfo.ifPresentOrElse(
                    $ -> info.getPostMethods().add(OneOf.left($)),
                    () -> info.getPostMethods().add(OneOf.right(path))
            );
            _env.addPost(path, webRequestProcessor, multipartSure);
        }

        public void addGet(String path, C1<WebRequestOuterFullContext> webRequestProcessor, O<WebMethodInfo> methodInfo) {
            methodInfo.ifPresentOrElse(
                    $ -> info.getGetMethods().add(OneOf.left($)),
                    () -> info.getGetMethods().add(OneOf.right(path))
            );
            _env.addGet(path, webRequestProcessor);
        }
    }

    @RequiredArgsConstructor
    private static class WebServerInfo {
        final String apiClassName;
        @Getter final List<OneOf<WebMethodInfo, String>> getMethods = Cc.l();
        @Getter final List<OneOf<WebMethodInfo, String>> postMethods = Cc.l();

        final F1<List<OneOf<WebMethodInfo, String>>, SortedMap<String, List<String>>> converter = x -> x.stream()
                .map($ -> X.x(
                        $.collect(w -> w.getFullMethodPath() + "(" + Cc.join(", ", w.getParamAndTypes(),
                                pit -> subLL(pit.getType().getType().getTypeName(), ".") + " " + pit.getName()) + ")",
                                w -> w),
                        $.collect(w -> w.getPrecompiledModel().getExceptions().stream().map(y -> y.getExceptionName())
                                .sorted()
                                .collect(Cc.toL()), Cc::l)
                )).collect(Collectors.toMap(X2::i1, X2::i2, (a, b) -> a, TreeMap::new));

        public WebServerShortInfo toShortInfo() {
            return new WebServerShortInfo(apiClassName, converter.apply(getMethods), converter.apply(postMethods));
        }
    }

    @RequiredArgsConstructor
    public static class WebServerShortInfo {
        final String apiClassName;
        final SortedMap<String, List<String>> getMethods;
        final SortedMap<String, List<String>> postMethods;
    }
}
