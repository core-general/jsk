package sk.web.server.spark.context;

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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.RuntimeIOException;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.springframework.core.annotation.Order;
import sk.exceptions.JskProblem;
import sk.services.bytes.IBytes;
import sk.services.except.IExcept;
import sk.services.ids.IIds;
import sk.services.ipgeo.IIpGeoExtractor;
import sk.services.json.IJson;
import sk.services.profile.IAppProfile;
import sk.utils.functional.C1;
import sk.utils.functional.F0;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.javafixes.CheckUtf8;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;
import sk.utils.statics.St;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;
import sk.web.exceptions.IWebExcept;
import sk.web.redirect.WebRedirectResult;
import sk.web.renders.WebContentTypeMeta;
import sk.web.renders.WebRenderResult;
import sk.web.renders.WebReplyMeta;
import sk.web.server.WebServerContext;
import sk.web.server.WebServerCore;
import sk.web.server.context.WebRequestIp;
import sk.web.server.context.WebRequestOuterFullContext;
import sk.web.server.model.WebProblemWithRequestBodyException;
import sk.web.server.params.WebAdditionalParams;
import sk.web.server.params.WebServerParams;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Service;
import spark.servlet.SparkApplication;
import spark.servlet.SparkFilter;

import javax.servlet.DispatcherType;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static sk.utils.functional.O.empty;
import static sk.utils.functional.O.ofNull;
import static sk.utils.statics.Io.streamPump;
import static sk.utils.statics.St.bytesToS;

@Order(1)
@Slf4j
public class WebJettyContextConsumer4Spark implements WebJettyContextConsumer, SparkApplication {
    @Inject WebServerParams conf;
    @Inject WebAdditionalParams additional;
    @Inject IExcept except;
    @Inject IBytes bytes;
    @Inject IJson json;
    @Inject IIds ids;
    @Inject IWebExcept webExcept;
    @Inject IAppProfile<?> profile;
    @Inject List<WebServerCore> serverDefinitions = Cc.l();

    @Inject protected Optional<IIpGeoExtractor> geoService = Optional.empty();

    @Override
    public void accept(ServletContextHandler context) {
        context.addFilter(new FilterHolder(new SparkFilter() {
                    @Override
                    protected SparkApplication[] getApplications(FilterConfig filterConfig) throws ServletException {
                        return new SparkApplication[]{WebJettyContextConsumer4Spark.this};
                    }
                }),
                "/*",
                EnumSet.of(DispatcherType.INCLUDE, DispatcherType.REQUEST));
    }

    private final Map<String, Class> gets = Cc.m();
    private final Map<String, Class> posts = Cc.m();

    @Override
    public void init() {
        Service spark = Service.ignite();
        spark.port(conf.getPort());
        conf.getStaticFilesLocation().ifPresent(sf -> {
            sf.getEitherResourceOrExternal().apply(
                    spark::staticFileLocation,
                    spark::externalStaticFileLocation
            );
        });

        for (WebServerCore definition : serverDefinitions) {
            final WebServerContext env = provideContext(spark, definition.getApiClass());
            definition.create(env);
        }

        spark.options("/*", new BasicSparkRoute("OPTIONS", "/*", false, ctx -> {
            additional.getCrossOrigin(ctx.getRequestHeader("Origin")).ifPresent($ -> {
                ctx.setResponseHeader("Access-Control-Allow-Origin", $);
                ctx.setResponseHeader("Access-Control-Allow-Credentials", "true");
            });
            ctx.setResponse(
                    new WebRenderResult(
                            new WebReplyMeta(200, new WebContentTypeMeta("text/html; charset=UTF-8"), false, false),
                            OneOf.left("")),
                    empty());
        }));

        spark.exception(Exception.class, (exception, request, response) -> {
            log.error("UNHANDLED EXCEPTION", exception);
            response.header(JskProblem.PROBLEM_SIGN, "+");
            response.status(500);
            response.body("OOPS! Please contact responsible person!");
        });
    }

    public WebServerContext provideContext(Service spark, Class apiClass) {
        return new WebServerContext() {
            @Override
            public void addPost(String path, C1<WebRequestOuterFullContext> webRequestProcessor, boolean multipartSure) {
                Cc.compute(posts, path, (k, v) -> except
                                .throwByDescription("Post method already exists:" + path + " in class:" + apiClass.getTypeName()),
                        () -> apiClass);
                spark.post(path, new BasicSparkRoute("POST", path, multipartSure, webRequestProcessor));
            }

            @Override
            public void addGet(String path, C1<WebRequestOuterFullContext> webRequestProcessor) {
                Cc.compute(gets, path, (k, v) -> except
                                .throwByDescription("Get method already exists:" + path + " in class:" + apiClass.getTypeName()),
                        () -> apiClass);
                spark.get(path, new BasicSparkRoute("GET", path, false, webRequestProcessor));
            }
        };
    }

    private class BasicSparkRoute implements Route {
        private final String type;
        private final String path;
        private final boolean multipartSure;
        private final C1<WebRequestOuterFullContext> webRequestProcessor;

        public BasicSparkRoute(String type, String path, boolean multipartSure,
                C1<WebRequestOuterFullContext> webRequestProcessor) {
            this.type = type;
            this.path = path;
            this.multipartSure = multipartSure;
            this.webRequestProcessor = webRequestProcessor;
        }

        @Override
        public Object handle(Request request, Response response) throws Exception {
            final SparkWebRequestOuterFullContext fullCtx =
                    new SparkWebRequestOuterFullContext(type, path, request, multipartSure, response, geoService, bytes, ids);
            //fullCtx.getParamAsBytes("__no_param_exist__");//to activate mulipart
            webRequestProcessor.accept(fullCtx);
            return response.body();
        }
    }

    private class SparkWebRequestOuterFullContext extends WebRequestOuterFullContext {
        public static final String JSK_USR_TOKEN = "_JSK_UT";
        public static final String JSK_CLIENT_TOKEN = "_JSK_CT";
        public static final String JSK_CLIENT_ID = "_JSK_CID";
        private final String type;
        private final String path;
        private final Request request;
        private final boolean multipartSure;
        private final Response response;
        private final Optional<IIpGeoExtractor> geoService;
        private final IBytes bytes;
        private final ConcurrentHashMap<String, byte[]> multipartCache;
        private final ConcurrentHashMap<String, String> paramCache;
        private final AtomicReference<String> requestHash = new AtomicReference<>();

        public SparkWebRequestOuterFullContext(String type, String path, Request request, boolean multipartSure,
                Response response, Optional<IIpGeoExtractor> geoService, IBytes bytes, IIds ids) {
            this.type = type;
            this.path = path;
            this.request = request;
            this.multipartSure = multipartSure;
            this.response = response;
            this.geoService = geoService;
            this.bytes = bytes;
            multipartCache = new ConcurrentHashMap<>();
            paramCache = new ConcurrentHashMap<>();
        }

        @Override
        public String getRequestType() {
            return type;
        }

        @Override
        public String getUrlPathPart() {
            return path;
        }

        @Override
        public WebRequestIp getFullIpInfo() {
            final String ip = request.ip();
            final O<String> proxy1 = getRequestHeader("X-Forwarded-For");
            final O<String> proxy2 = getRequestHeader("X-Forwarded-For-1");

            List<String> proxies = Cc.l();
            proxy2.ifPresentOrElse(w -> {
                proxy1.ifPresent(proxies::add);
                proxies.add(ip);
            }, () -> {
                proxy1.ifPresent((x) -> proxies.add(ip));
            });

            final String realIp = proxy2.or(() -> proxy1).orElse(ip);
            return new WebRequestIp(realIp, proxies,
                    O.of(geoService).flatMap($ -> $.ipToGeoData(realIp)));
        }

        @Override
        public SortedSet<String> getRequestHeaderNames() {
            return throwOnBadRequestData(() -> {
                return new TreeSet<>(request.headers());
            });
        }

        @Override
        public Map<String, String> getAllParamsAsStrings() {
            return throwOnBadRequestData(() -> {
                return Cc.putAll(request.queryParams().stream()
                        .map($ -> X.x($, request.queryParams($)))
                        .collect(Cc.toMX2()), request.params());
            });
        }

        @Override
        public O<String> getRequestHeader(String name) {
            return throwOnBadRequestData(() -> {
                return ofNull(request.headers(name));
            });
        }

        @Override
        public SortedSet<String> getResponseHeaderNames() {
            return throwOnBadRequestData(() -> {
                return new TreeSet<>(response.raw().getHeaderNames());
            });
        }

        @Override
        public O<String> getResponseHeader(String name) {
            return throwOnBadRequestData(() -> {
                return ofNull(response.raw().getHeader(name));
            });
        }

        @Override
        public boolean isMultipart() {
            return throwOnBadRequestData(() -> {
                HttpServletRequest rawRequest = request.raw();
                return rawRequest.getContentType() != null
                       && rawRequest.getContentType().startsWith("multipart/form-data");
            });
        }

        @Override
        public O<String> getParamAsString(String param) {
            final String valk = paramCache.computeIfAbsent(param, k -> throwOnBadRequestData(() -> {
                if (multipartSure) {
                    return ofNull(multipart(request, param).map(bytes -> bytesToS(bytes, "UTF-8"))
                            .orElse(null));
                } else {
                    return ofNull(request.params(param))
                            .or(() -> ofNull(request.queryParams(param)))
                            .or(() -> multipart(request, param)
                                    .map(bytes -> bytesToS(bytes, "UTF-8")));
                }
            }).orElse(null));
            return ofNull(valk);
        }

        @Override
        public O<byte[]> getParamAsBytes(String param) {
            return throwOnBadRequestData(() -> multipart(request, param));
        }

        @Override
        public O<byte[]> getBody() {
            return throwOnBadRequestData(() -> {
                final byte[] value = request.bodyAsBytes();
                if (value.length == 0) {
                    log.error("Body has size 0, we assume it doesn't exist! Method:" + path);
                    return empty();
                } else {
                    return O.ofNull(value);
                }
            });
        }

        @Override
        public SortedMap<String, String> getNonMultipartParamInfo() {
            return throwOnBadRequestData(() -> {
                TreeMap<String, String> toRet = new TreeMap<>();
                if (isMultipart()) {
                    for (String paramName : Cc.enumerableToIterable(request.raw().getParameterNames())) {
                        getParamAsBytes(paramName)
                                .filter($ -> CheckUtf8.isUtf8String($))
                                .flatMap($ -> getParamAsString(paramName))
                                .ifPresent($ -> toRet.put(paramName, $));
                    }
                } else {
                    for (String paramName : Cc.enumerableToIterable(request.raw().getParameterNames())) {
                        toRet.put(paramName, request.queryParams(paramName));
                    }
                }

                toRet.putAll(request.params());
                return toRet;
            });
        }

        @Override
        public O<List<Part>> getMultipartParamInfo() {
            return throwOnBadRequestData(() -> {
                try {
                    if (isMultipart()) {
                        List<Part> parts = new ArrayList<>();
                        for (String paramName : Cc.enumerableToIterable(request.raw().getParameterNames())) {
                            if (getParamAsBytes(paramName).filter($ -> CheckUtf8.isBinary($)).isPresent()) {
                                parts.add(request.raw().getPart(paramName));
                            }
                        }
                        return ofNull(Cc.sort(parts, Comparator.comparing($ -> $.getName())));
                    } else {
                        return empty();
                    }
                } catch (IOException e) {
                    throw new WebProblemWithRequestBodyException(e);
                } catch (ServletException e) {
                    return empty();
                }
            });
        }

        @Override
        public String getRequestHash() {
            return requestHash.updateAndGet(old -> {
                if (old == null) {
                    final SortedMap<String, Long> params = getNonMultipartParamInfo().entrySet().stream()
                            .map($ -> X.x($.getKey(), bytes.crc32($.getValue())))
                            .collect(Collectors.toMap($ -> $.i1(), $ -> $.i2(), Cc.throwingMerger(), TreeMap::new));
                    getMultipartParamInfo().ifPresent($ -> $.stream()
                            .map($$ -> X.x($$.getName(), getParamAsBytes($$.getName())
                                    .map($$$ -> bytes.crc32($$$))
                                    .orElse(0L)))
                            .forEach($$ -> params.put($$.i1(), $$.i2())));

                    final String toHash = getUrlPathPart() + Cc.joinMap("", "", params);
                    final long hash = bytes.crc32(toHash);
                    return hash + "";
                } else {
                    return old;
                }
            });
        }

        @Override
        public O<String> getRequestToken() {
            return conf.isUseCookiesForToken() ? O.ofNull(request.cookie(JSK_USR_TOKEN)) : empty();
        }

        @Override
        public boolean setResponseToken(String token) {
            if (conf.isUseCookiesForToken()) {
                response.cookie("/", JSK_USR_TOKEN, token, conf.getTokenTimeoutSec().orElse(Integer.MAX_VALUE),
                        profile.getProfile().isForProductionUsage(), true);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public X2<String, String> getClientIdAndTokenCookie(String saltPassword) {
            final O<X2<String, String>> val = O.allNotNull(
                    ofNull(request.cookie(JSK_CLIENT_ID)), ofNull(request.cookie(JSK_CLIENT_TOKEN)),
                    (a, b) -> O.of(X.x(a, b)));

            return val.orElseGet(() -> {
                final WebRequestIp fullIpInfo = getFullIpInfo();
                final String clientIp = fullIpInfo.getClientIp();

                final String id = ids.customId(32);
                response.cookie("/", JSK_CLIENT_ID,
                        id,
                        conf.getTokenTimeoutSec().orElse(Integer.MAX_VALUE),
                        profile.getProfile().isForProductionUsage(), true);

                final String token = bytes.enc62(bytes.sha256((clientIp + saltPassword).getBytes(StandardCharsets.UTF_8)));
                response.cookie("/", JSK_CLIENT_TOKEN,
                        token,
                        conf.getTokenTimeoutSec().orElse(Integer.MAX_VALUE),
                        profile.getProfile().isForProductionUsage(), true);


                return X.x(id, token);
            });
        }

        @Override
        public void redirect(String url) {
            response.redirect(url);
        }

        @Override
        public void setCookie(String path, String key, String value, int seconds, boolean httpOnly) {
            response.cookie(path, key, value, seconds, profile.getProfile().isForProductionUsage(), httpOnly);
        }

        @Override
        public O<String> getCookie(String key) {
            return ofNull(request.cookie(key));
        }

        @Override
        public void deleteCookie(String key) {
            response.removeCookie(key);
        }

        @Override
        public void setResponseHeader(String key, String value) {
            response.header(key, value);
        }

        @Override
        protected void innerSetResponse(WebRenderResult result, O<WebRedirectResult> oRedirect) {
            if (oRedirect.isPresent()) {
                final WebRedirectResult redirect = oRedirect.get();
                StringBuilder sb = new StringBuilder(St.notEndWith(redirect.getPath(), "/"));
                if (redirect.isAddModelFieldsAsRedirectParameters()) {
                    result.getValue().ifLeft(v -> {
                        try {
                            final Map<String, Object> dict = json.from(v, TypeWrap.getMap(String.class, Object.class));
                            boolean hasVariables = redirect.getPath().contains("?");
                            for (Map.Entry<String, Object> dictVals : dict.entrySet()) {
                                //!only in case of string we add it to redirect!
                                if (dictVals.getValue() instanceof String) {
                                    sb.append(!hasVariables ? "?" : "&")
                                            .append(dictVals.getKey())
                                            .append("=")
                                            .append(dictVals.getValue());
                                    hasVariables = true;
                                }
                            }
                        } catch (Exception e) {
                            log.error("Can't deserialize " + St.raze(v, 100) + " as json object for redirect. Path: " + path +
                                      ". Error message : " +
                                      e.getMessage());
                        }
                    });
                }
                redirect(sb.toString());
            } else {
                response.status(result.getMeta().getHttpCode());
                response.type(result.getMeta().getContentType().getContentType());
                result.getMeta().getContentType().getFileName().ifPresent(file -> {
                    response.header("Content-Disposition", String.format("attachment; filename=\"%s\"", file));
                });
                if (result.getMeta().isAllowDeflation() && requestHeaderAllowDeflation()) {
                    response.header("Content-Encoding", "gzip");
                }

                if (response.body() == null) {
                    result.getValue().apply(
                            string -> {
                                response.body(string);
                            },
                            bytes -> {
                                response.header("Content-Length", bytes.length + "");
                                try (ServletOutputStream stream = response.raw().getOutputStream();
                                     BufferedOutputStream bos = new BufferedOutputStream(stream)) {
                                    bos.write(bytes);
                                } catch (Exception e) {
                                    log.error("", e);
                                }
                            }
                    );
                } else {
                    log.error("BODY IS ALREADY SET FOR ENDPOINT:" + path);
                }
            }
        }

        private boolean requestHeaderAllowDeflation() {
            return getRequestHeader("Accept-Encoding")
                    .stream()
                    .flatMap($ -> Cc.stream($.split(",")))
                    .map($ -> $.toLowerCase().trim())
                    .anyMatch($ -> Fu.equal($, "gzip"));
        }

        private O<byte[]> multipart(Request req, String paramName) {
            return isMultipart()
                   ? ofNull(
                    multipartCache.computeIfAbsent(paramName, (k) -> {
                        try {
                            return O.ofNull(req.raw().getPart(paramName)).flatMap(p -> {
                                long size = p.getSize();
                                byte[] bytes = new byte[0];
                                try {
                                    bytes = streamPump(p.getInputStream());
                                } catch (IOException e) {
                                    return empty();
                                }
                                if (size != bytes.length) {
                                    return except.throwByDescription(
                                            "Data is not uploaded fully: expected size:" + size + " != obtained size:" +
                                            bytes.length);
                                }
                                return ofNull(bytes);
                            }).orElse(null);
                        } catch (IOException e) {
                            throw new WebProblemWithRequestBodyException(e);
                        } catch (ServletException e) {
                            return null;
                        }
                    }))
                   : empty();
        }

        private <T> T throwOnBadRequestData(F0<T> toRun) {
            try {
                return toRun.apply();
            } catch (RuntimeIOException e) {
                throw new WebProblemWithRequestBodyException(e);
            }
        }
    }
}
