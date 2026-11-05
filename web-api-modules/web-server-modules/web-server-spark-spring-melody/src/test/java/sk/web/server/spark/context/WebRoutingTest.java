package sk.web.server.spark.context;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2026 Core General
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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sk.services.CoreServicesRaw;
import sk.services.ICoreServices;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.javafixes.TypeWrap;
import sk.web.WebMethodType;
import sk.web.annotations.WebBody;
import sk.web.annotations.WebPath;
import sk.web.annotations.type.WebDELETE;
import sk.web.annotations.type.WebGET;
import sk.web.annotations.type.WebPATCH;
import sk.web.annotations.type.WebPOST;
import sk.web.annotations.type.WebPUT;
import sk.web.client.WebClientFactory;
import sk.web.client.WebClientResultHandlerBase;
import sk.web.client.WebRequestResultModel;
import sk.web.client.java.WebJavaSpecificHelper;
import sk.web.exceptions.IWebExcept;
import sk.web.exceptions.WebParameterException;
import sk.web.infogatherer.WebClassInfo;
import sk.web.infogatherer.WebClassInfoProvider;
import sk.web.infogatherer.WebMethodInfo;
import sk.web.renders.WebContentTypeMeta;
import sk.web.renders.WebFilterOutput;
import sk.web.renders.WebRender;
import sk.web.renders.WebRenderResult;
import sk.web.renders.WebReplyMeta;
import sk.web.renders.inst.WebRawStringRender;
import sk.web.server.WebServerCore;
import sk.web.server.WebUnmatchedRequestHandler;
import sk.web.server.context.WebRequestOuterFullContext;
import sk.web.server.filters.WebServerFilter;
import sk.web.server.filters.WebServerFilterContext;
import sk.web.server.params.WebExceptionParams;
import sk.web.server.params.WebServerParams;
import sk.web.server.spark.WebJettyServerStarter;
import sk.web.utils.WebUtils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WebRoutingTest {
    private static final ICoreServices services = CoreServicesRaw.services();
    private static final String PREFIX = "/api";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Map<String, List<String>> ITEM_PARAMETERS = Map.of(
            "read", List.of("item_id", "view"),
            "replace", List.of("item_id", "body", "mode"),
            "amend", List.of("item_id", "body"),
            "remove", List.of("item_id", "dry_run"),
            "note", List.of("item_id", "body"),
            "count", List.of("page"),
            "shout", List.of("word"));
    private static WebJettyServerStarter starter;
    private static String baseUrl;

    @BeforeAll
    static void start() throws IOException {
        int port = availablePort();
        WebJettyContextConsumer4Spark consumer = new WebJettyContextConsumer4Spark();
        consumer.conf = params(port);
        consumer.additional = origin -> O.empty();
        consumer.except = services.except();
        consumer.bytes = services.bytes();
        consumer.json = services.json();
        consumer.ids = services.ids();
        consumer.webExcept = () -> new WebRawStringRender();
        consumer.serverDefinitions = List.of(
                new StubServer<>(ItemsApi.class, new Items(), ITEM_PARAMETERS),
                new StubServer<>(LineupApi.class, () -> "lineup", Map.of("lineup", List.of())));
        consumer.unmatchedHandlers = Optional.of(List.of(new EchoUnmatched()));
        starter = new WebJettyServerStarter(params(port), List.of(consumer));
        starter.run();
        baseUrl = "http://127.0.0.1:" + port;
    }

    @AfterAll
    static void stop() {
        starter.onStop();
    }

    @Test
    void eachMethodReachesItsEndpointWithPathQueryAndBody() throws Exception {
        assertEquals("read 7 full", call("GET", "/api/items/7?view=full", null).body());
        assertEquals("replace 7 {\"a\":1} strict", call("PUT", "/api/items/7?mode=strict", "{\"a\":1}").body());
        assertEquals("amend 7 {\"b\":2}", call("PATCH", "/api/items/7", "{\"b\":2}").body());
        assertEquals("remove 7 true", call("DELETE", "/api/items/7?dry_run=true", null).body());
        assertEquals("note 7 hello", call("POST", "/api/items/7/notes", "hello").body());
    }

    @Test
    void typedClientsSendEveryMethodWithEncodedPathQueryAndBody() {
        ItemsApi items = typedClient();

        assertEquals("read 7 full", items.read("7", O.of("full")));
        assertEquals("replace 7 x {\"a\":1} strict", items.replace("7 x", "{\"a\":1}", O.of("strict")));
        assertEquals("amend 7 {\"b\":2}", items.amend("7", "{\"b\":2}"));
        assertEquals("remove 7 true", items.remove("7", O.of(true)));
        assertEquals("note 7?8 hello", items.note("7?8", "hello"));
    }

    @Test
    void literalSegmentsWinOverParametersWhicheverServerRegisteredFirst() throws Exception {
        assertEquals("lineup", call("GET", "/api/items/lineup", null).body());
        assertEquals("read other none", call("GET", "/api/items/other", null).body());
    }

    @Test
    void missingAndMalformedParametersReachTheServerAsTypedFailures() throws Exception {
        assertEquals("MISSING page", call("GET", "/api/counts", null).body());
        assertEquals("MALFORMED page x", call("GET", "/api/counts?page=x", null).body());
        assertEquals("MISSING body", call("PATCH", "/api/items/7", null).body());
    }

    @Test
    void serverConvertersReplaceTheDefaultParsing() throws Exception {
        assertEquals("HEY", call("GET", "/api/shout?word=hey", null).body());
    }

    @Test
    void unmatchedRequestsUnderThePrefixReachTheHandlerWithTheMethodsThatMatch() throws Exception {
        HttpResponse<String> unknown = call("GET", "/api/nothing/here", null);
        HttpResponse<String> wrongMethod = call("POST", "/api/items/7", "x");

        assertEquals("GET /api/nothing/here []", unknown.body());
        assertEquals(EchoUnmatched.NOT_FOUND, unknown.statusCode());
        assertEquals("POST /api/items/7 [DELETE, GET, PATCH, PUT]", wrongMethod.body());
        assertEquals(EchoUnmatched.NOT_ALLOWED, wrongMethod.statusCode());
    }

    @Test
    void requestsOutsideThePrefixAreLeftToTheContainer() throws Exception {
        HttpResponse<String> outside = call("GET", "/elsewhere", null);

        assertFalse(outside.body().contains("/elsewhere ["));
        assertEquals(EchoUnmatched.NOT_FOUND, outside.statusCode());
    }

    private static ItemsApi typedClient() {
        WebClientResultHandlerBase rawText = new WebClientResultHandlerBase() {
            {
                json = services.json();
                except = services.except();
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T ifNotBusinessProblemAndNotByteArr(String val, WebRequestResultModel<T> resultModel) {
                return (T) val;
            }
        };
        return new WebClientFactory(stubInfo(ITEM_PARAMETERS), services.http(), services.json(), services.ids(),
                services.repeat(), services.except(), null, new WebJavaSpecificHelper())
                .createWebApiClient(baseUrl + PREFIX, ItemsApi.class, rawText).get();
    }

    private static WebClassInfoProvider stubInfo(Map<String, List<String>> parameterNames) {
        return new WebClassInfoProvider() {
            @Override
            public <A> WebClassInfo getClassModel(Class<A> apiCls, O<String> basePath) {
                Map<String, WebMethodInfo> methods = Arrays.stream(WebUtils.getActualApiMethods(apiCls))
                        .collect(Collectors.toMap(Method::getName,
                                $ -> methodInfo($, basePath, parameterNames.get($.getName()))));
                return new WebClassInfo(PREFIX, apiCls.getName(), O.empty(), methods, Map.of());
            }
        };
    }

    private static WebMethodInfo methodInfo(Method method, O<String> basePath, List<String> names) {
        List<WebMethodInfo.ParameterNameAndType> parameters = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            parameters.add(new WebMethodInfo.ParameterNameAndType(names.get(i),
                    TypeWrap.raw(method.getGenericParameterTypes()[i]), false,
                    method.getParameters()[i].isAnnotationPresent(WebBody.class)));
        }
        return new WebMethodInfo(method, WebUtils.getMethodApiPath(basePath, method), methodType(method),
                new WebMethodInfo.ParameterNameAndType("return", TypeWrap.raw(method.getGenericReturnType()), false),
                parameters, null, O.empty(), O.empty());
    }

    private static WebMethodType methodType(Method method) {
        if (method.isAnnotationPresent(WebPUT.class)) {
            return WebMethodType.PUT;
        } else if (method.isAnnotationPresent(WebPATCH.class)) {
            return WebMethodType.PATCH;
        } else if (method.isAnnotationPresent(WebDELETE.class)) {
            return WebMethodType.DELETE;
        } else if (method.isAnnotationPresent(WebPOST.class)) {
            return WebMethodType.POST_MULTI;
        }
        return WebMethodType.GET;
    }

    private static HttpResponse<String> call(String method, String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(10))
                        .method(method, body == null
                                        ? HttpRequest.BodyPublishers.noBody()
                                        : HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    public record Shout(String word) {}

    public interface ItemsApi {
        @WebGET @WebPath(value = "items/:item_id", appendMethodName = false)
        String read(String itemId, O<String> view);

        @WebPUT @WebPath(value = "items/:item_id", appendMethodName = false)
        String replace(String itemId, @WebBody String body, O<String> mode);

        @WebPATCH @WebPath(value = "items/:item_id", appendMethodName = false)
        String amend(String itemId, @WebBody String body);

        @WebDELETE @WebPath(value = "items/:item_id", appendMethodName = false)
        String remove(String itemId, O<Boolean> dryRun);

        @WebPOST @WebPath(value = "items/:item_id/notes", appendMethodName = false)
        String note(String itemId, @WebBody String body);

        @WebGET @WebPath(value = "counts", appendMethodName = false)
        String count(int page);

        @WebGET @WebPath(value = "shout", appendMethodName = false)
        String shout(Shout word);
    }

    public interface LineupApi {
        @WebGET @WebPath(value = "items/lineup", appendMethodName = false)
        String lineup();
    }

    public static class Items implements ItemsApi {
        @Override public String read(String itemId, O<String> view) {return "read " + itemId + " " + view.orElse("none");}

        @Override public String replace(String itemId, String body, O<String> mode) {
            return "replace " + itemId + " " + body + " " + mode.orElse("none");
        }

        @Override public String amend(String itemId, String body) {return "amend " + itemId + " " + body;}

        @Override public String remove(String itemId, O<Boolean> dryRun) {return "remove " + itemId + " " + dryRun.orElse(false);}

        @Override public String note(String itemId, String body) {return "note " + itemId + " " + body;}

        @Override public String count(int page) {return "count " + page;}

        @Override public String shout(Shout word) {return word.word();}
    }

    static class EchoUnmatched implements WebUnmatchedRequestHandler {
        static final int NOT_FOUND = 404;
        static final int NOT_ALLOWED = 405;

        @Override
        public String getPathPrefix() {
            return PREFIX;
        }

        @Override
        public void handle(String httpMethod, String path, List<String> methodsMatchingPath, WebRequestOuterFullContext context) {
            String allowed = methodsMatchingPath.stream().sorted().collect(Collectors.joining(", ", "[", "]"));
            context.setResponse(new WebRenderResult(
                    new WebReplyMeta(methodsMatchingPath.isEmpty() ? NOT_FOUND : NOT_ALLOWED,
                            new WebContentTypeMeta("text/plain"), false, false),
                    OneOf.left(httpMethod + " " + path + " " + allowed)), O.empty());
        }
    }

    static class StubServer<API> extends WebServerCore<API> {
        private final WebRender render = new WebRawStringRender();

        StubServer(Class<API> apiClass, API implementation, Map<String, List<String>> parameterNames) {
            super(apiClass, implementation, PREFIX);
            infoProvider = stubInfo(parameterNames);
            webExcept = () -> render;
            ids = services.ids();
            time = services.times();
            json = services.json();
            except = services.except();
            apiInfoParams = Optional.empty();
            exceptConf = new WebExceptionParams() {
                @Override
                public int getUnhandledJskExceptionHttpCode() {
                    return 500;
                }

                @Override
                public int getUnknownExceptionHttpCode() {
                    return 500;
                }
            };
        }

        @Override
        protected List<WebServerFilter> getDefaultFilters() {
            return List.of(new FailureEcho());
        }

        @Override
        protected O<? extends WebRender> getDefaultRender() {
            return O.of(render);
        }

        @Override
        protected O<F1<String, Object>> getParameterConverter(String paramName, Type type, boolean isBody) {
            return type == Shout.class ? O.of(raw -> new Shout(raw.toUpperCase())) : O.empty();
        }
    }

    static class FailureEcho implements WebServerFilter {
        @Override
        public int getFilterPriority() {
            return 0;
        }

        @Override
        public <API> WebFilterOutput invoke(WebServerFilterContext<API> requestContext) {
            try {
                return requestContext.getNextInChain().invokeNext();
            } catch (WebParameterException failure) {
                return WebFilterOutput.rawValue(422, (failure.getKind() + " " + failure.getParameterName() + " " +
                                                      failure.getRawValue().orElse("")).trim());
            }
        }
    }

    private static WebServerParams params(int port) {
        return new WebServerParams() {
            @Override
            public String getServerNameHeader() {
                return "JSK-Test";
            }

            @Override
            public int getPort() {
                return port;
            }

            @Override
            public O<Long> getShutdownWait() {
                return O.empty();
            }

            @Override
            public long getFormLimit() {
                return 1_000_000;
            }

            @Override
            public O<Long> getIdleTimeout() {
                return O.empty();
            }

            @Override
            public O<WebStaticFiles> getStaticFilesLocation() {
                return O.empty();
            }

            @Override
            public O<Integer> getTokenTimeoutSec() {
                return O.empty();
            }

            @Override
            public boolean isUseCookiesForToken() {
                return false;
            }
        };
    }

    private static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
