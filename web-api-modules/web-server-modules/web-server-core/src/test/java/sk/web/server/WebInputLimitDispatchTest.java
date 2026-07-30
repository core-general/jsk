package sk.web.server;

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

import jakarta.servlet.http.Part;
import org.junit.jupiter.api.Test;
import sk.services.ids.IIds;
import sk.services.ids.JskHaikunator;
import sk.utils.functional.C1;
import sk.utils.functional.O;
import sk.utils.javafixes.TypeWrap;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;
import sk.web.WebMethodType;
import sk.web.annotations.WebInputLimit;
import sk.web.annotations.type.WebPOST;
import sk.web.exceptions.IWebExcept;
import sk.web.infogatherer.WebClassInfo;
import sk.web.infogatherer.WebClassInfoProvider;
import sk.web.infogatherer.WebMethodInfo;
import sk.web.redirect.WebRedirectResult;
import sk.web.renders.WebRender;
import sk.web.renders.WebRenderResult;
import sk.web.renders.inst.WebRawStringRender;
import sk.web.server.context.WebRequestIp;
import sk.web.server.context.WebRequestOuterFullContext;
import sk.web.server.filters.WebServerFilter;
import sk.web.server.model.WebInputLimitExceededException;
import sk.web.utils.WebUtils;

import java.awt.Color;
import java.lang.reflect.Method;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebInputLimitDispatchTest {
    @Test
    void translatesLimitFailureDuringContextPreparationToTyped413() {
        AtomicBoolean endpointInvoked = new AtomicBoolean();
        LimitedApi implementation = payload -> {
            endpointInvoked.set(true);
            return "accepted";
        };
        Method method = WebUtils.getActualApiMethods(LimitedApi.class)[0];
        WebMethodInfo methodInfo = new WebMethodInfo(
                method,
                "/limited/upload",
                WebMethodType.POST_MULTI_SURE,
                new WebMethodInfo.ParameterNameAndType("return", TypeWrap.simple(String.class), false),
                List.of(new WebMethodInfo.ParameterNameAndType(
                        "payload", TypeWrap.simple(byte[].class), false)),
                null,
                O.empty(),
                O.empty());
        WebClassInfo classInfo = new WebClassInfo(
                "/limited",
                LimitedApi.class.getName(),
                O.empty(),
                Map.of(method.getName(), methodInfo),
                Map.of());
        DispatchServer server = new DispatchServer(implementation, classInfo);
        AtomicReference<C1<WebRequestOuterFullContext>> processor = new AtomicReference<>();
        server.create(new WebServerContext() {
            @Override
            public void addPost(String path, C1<WebRequestOuterFullContext> value, boolean multipartSure) {
                processor.set(value);
            }

            @Override
            public void addGet(String path, C1<WebRequestOuterFullContext> value) {
                throw new AssertionError(path);
            }
        });
        CapturingContext context = new CapturingContext();

        processor.get().accept(context);

        assertTrue(context.limitInstalled.get());
        assertFalse(endpointInvoked.get());
        assertEquals(413, context.response.get().getMeta().getHttpCode());
        assertTrue(context.response.get().getMeta().isProblem());
    }

    private interface LimitedApi {
        @WebPOST(forceMultipart = true)
        @WebInputLimit(
                maxRequestBytes = 65_536,
                maxPartBytes = 49_152,
                maxAggregatePartBytes = 53_248,
                maxPartCount = 16)
        String upload(byte[] payload);
    }

    private static final class CapturingContext extends WebRequestOuterFullContext {
        private final AtomicBoolean limitInstalled = new AtomicBoolean();
        private final AtomicReference<WebRenderResult> response = new AtomicReference<>();

        @Override
        public void setInputLimit(WebInputLimit inputLimit) {
            limitInstalled.set(true);
        }

        @Override
        public WebRequestIp getFullIpInfo() {
            assertTrue(limitInstalled.get());
            throw new WebInputLimitExceededException("request_too_large", "Too large.");
        }

        @Override
        public String getRequestType() {
            return "POST";
        }

        @Override
        public String getUrlPathPart() {
            return "/limited/upload";
        }

        @Override
        public SortedSet<String> getRequestHeaderNames() {
            return new TreeSet<>();
        }

        @Override
        public O<String> getRequestHeader(String name) {
            return O.empty();
        }

        @Override
        public SortedSet<String> getResponseHeaderNames() {
            return new TreeSet<>();
        }

        @Override
        public O<String> getResponseHeader(String name) {
            return O.empty();
        }

        @Override
        public Map<String, String> getAllParamsAsStrings() {
            return Map.of();
        }

        @Override
        public boolean isMultipart() {
            return true;
        }

        @Override
        public O<String> getParamAsString(String param) {
            return O.empty();
        }

        @Override
        public O<byte[]> getParamAsBytes(String param) {
            return O.empty();
        }

        @Override
        public O<byte[]> getBody() {
            return O.empty();
        }

        @Override
        public SortedMap<String, String> getNonMultipartParamInfo() {
            return new TreeMap<>();
        }

        @Override
        public O<List<Part>> getMultipartParamInfo() {
            return O.empty();
        }

        @Override
        public String getRequestHash() {
            return "";
        }

        @Override
        public O<String> getRequestToken() {
            return O.empty();
        }

        @Override
        public boolean setResponseToken(String token) {
            return false;
        }

        @Override
        public X2<String, String> getClientIdAndTokenCookie(String saltPassword) {
            return X.x("", "");
        }

        @Override
        public void redirect(String url) {
        }

        @Override
        public void setCookie(String path, String key, String value, int seconds, boolean httpOnly) {
        }

        @Override
        public O<String> getCookie(String key) {
            return O.empty();
        }

        @Override
        public void deleteCookie(String key) {
        }

        @Override
        public void setResponseHeader(String key, String value) {
        }

        @Override
        public void innerSetResponse(WebRenderResult result, O<WebRedirectResult> redirect) {
            response.set(result);
        }
    }

    private static final class DispatchServer extends WebServerCore<LimitedApi> {
        private final WebRender render = new WebRawStringRender();

        DispatchServer(LimitedApi implementation, WebClassInfo classInfo) {
            super(LimitedApi.class, implementation);
            infoProvider = new WebClassInfoProvider() {
                @Override
                public <API> WebClassInfo getClassModel(Class<API> apiCls, O<String> basePath) {
                    return classInfo;
                }
            };
            webExcept = new IWebExcept() {
                @Override
                public WebRender getDefaultExceptionRender() {
                    return render;
                }
            };
            ids = new FixedIds();
            time = () -> ZoneOffset.UTC;
            apiInfoParams = Optional.empty();
        }

        @Override
        protected List<WebServerFilter> getDefaultFilters() {
            return List.of();
        }

        @Override
        protected O<? extends WebRender> getDefaultRender() {
            return O.of(render);
        }
    }

    private static final class FixedIds implements IIds {
        @Override
        public UUID shortId() {
            return new UUID(0, 1);
        }

        @Override
        public String customId(int length) {
            return "";
        }

        @Override
        public byte[] genUniquePngImageById(String id, int blockCount, int blockSize, Color bgColor) {
            return new byte[0];
        }

        @Override
        public byte[] genUniquePngImage(int blockCount, int blockSize, Color bgColor) {
            return new byte[0];
        }

        @Override
        public UUID byte2Uuid(byte[] val) {
            return new UUID(0, 1);
        }

        @Override
        public String unique(byte[] val, int rawByteSize) {
            return "";
        }

        @Override
        public String longHaiku() {
            return "";
        }

        @Override
        public String shortHaiku() {
            return "";
        }

        @Override
        public JskHaikunator.LongAndShortHaikunator defaultHaikunators() {
            return null;
        }

        @Override
        public String tinyHaiku() {
            return "";
        }

        @Override
        public String timedHaiku() {
            return "";
        }
    }
}
