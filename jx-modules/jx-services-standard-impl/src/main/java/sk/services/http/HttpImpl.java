package sk.services.http;

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

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import okhttp3.*;
import sk.exceptions.JskProblem;
import sk.services.http.model.CoreHttpResponse;
import sk.services.retry.IRepeat;
import sk.services.time.ITime;
import sk.utils.functional.F1;
import sk.utils.functional.F1E;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;
import sk.utils.tuples.X;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static sk.utils.functional.O.ofNullable;
import static sk.utils.statics.Ex.thRow;

@SuppressWarnings("unused")
@AllArgsConstructor
@NoArgsConstructor
public class HttpImpl implements IHttp {
    @Inject IRepeat retry;
    @Inject ITime times;

    protected OkHttpClient okHttpClient = prepareBuilder().build();

    protected OkHttpClient.Builder prepareBuilder() {
        return new OkHttpClient.Builder()
                .connectTimeout(getConfig().getConnectTimeout())
                .readTimeout(getConfig().getReadTimeout())
                .writeTimeout(getConfig().getReadTimeout())
                .protocols(Cc.l(getConfig().forceHttp2() ? Protocol.HTTP_2
                                                         : Protocol.HTTP_1_1))//for HTTP2 OkHTTP threads are not daemon on java 9+
                ;
    }

    @Override
    public F1<HttpGetBuilder, OneOf<CoreHttpResponse, Exception>> howToGet() {
        return getBuilder -> executeUni(getBuilder, this::executeSingleGet);
    }

    @Override
    public <T extends HttpPostBuilder<T>> F1<T, OneOf<CoreHttpResponse, Exception>> howToPost() {
        return postBuilder -> executeUni(postBuilder, this::executeSinglePost);
    }

    @Override
    public <T extends HttpPostBuilder<T>> F1<T, OneOf<CoreHttpResponse, Exception>> howToDelete() {
        return postBuilder -> executeUni(postBuilder, this::executeSingleDelete);
    }

    @Override
    public F1<HttpHeadBuilder, OneOf<CoreHttpResponse, Exception>> howToHead() {
        return headBuilder -> executeUni(headBuilder, this::executeSingleHead);
    }

    private <T extends HttpBuilder<T>> OneOf<CoreHttpResponse, Exception> executeUni(T builder,
            F1E<T, CoreHttpResponse> oneTimeGetter) {
        try {
            return OneOf.left(retry.repeatE(
                    () -> {
                        CoreHttpResponse resp = oneTimeGetter.apply(builder);
                        if (resp.code() == 502 || resp.code() == 503 || resp.code() == 504) {
                            throw new RetryException(resp);
                        }
                        return resp;
                    },
                    builder.tryCount(),
                    builder.trySleepMs(),
                    Cc.s(IOException.class, RetryException.class)));
        } catch (Exception e) {
            return OneOf.right(e);
        }
    }

    private <T extends HttpPostBuilder<T>> CoreHttpResponse executeSinglePost(T postBuilder) throws IOException {
        RequestBody rb = definePostRequestBody(postBuilder);

        return execute(postBuilder, new Request.Builder()
                .url(postBuilder.url())
                .post(rb), false);
    }

    private <T extends HttpPostBuilder<T>> CoreHttpResponse executeSingleDelete(T postBuilder) throws IOException {
        RequestBody rb = definePostRequestBody(postBuilder);

        return execute(postBuilder, new Request.Builder()
                .url(postBuilder.url())
                .delete(rb), false);
    }

    private <T extends HttpPostBuilder<T>> RequestBody definePostRequestBody(T pb) {
        switch (pb.getType()) {
            case BODY:
                return ((HttpBodyBuilder) pb).body().collect($ -> RequestBody.create(null, $), $ -> RequestBody.create(null, $));
            case FORM:
                FormBody.Builder formBuilder = new FormBody.Builder();
                ofNullable(((HttpFormBuilder) pb).parameters()).ifPresent($ -> $.forEach(formBuilder::add));
                return formBuilder.build();
            case MULTIPART:
                MultipartBody.Builder multiBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                ofNullable(((HttpMultipartBuilder) pb).parameters()).ifPresent($ -> $.forEach(multiBuilder::addFormDataPart));
                ofNullable(((HttpMultipartBuilder) pb).rawParameters()).ifPresent(
                        $ -> $.forEach((k, v) -> multiBuilder.addFormDataPart(k, null, RequestBody.create(null, v))));
                return multiBuilder.build();
        }
        return thRow(pb.getType() + " unknown");
    }

    private CoreHttpResponse executeSingleGet(HttpGetBuilder getBuilder) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(getBuilder.url())
                .get();
        return execute(getBuilder, builder, false);
    }

    private CoreHttpResponse executeSingleHead(HttpHeadBuilder headBuilder) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(headBuilder.url())
                .head();
        return execute(headBuilder, builder, true);
    }


    private <T extends HttpBuilder<T>> CoreHttpResponse execute(HttpBuilder<T> xBuilder, Request.Builder builder,
            boolean forceEmptyContent)
            throws IOException {
        long start = times.now();
        if (xBuilder.login() != null && xBuilder.password() != null) {
            builder.header("Authorization", Credentials.basic(xBuilder.login(), xBuilder.password()));
        }
        ofNullable(xBuilder.headers()).ifPresent($ -> $.forEach(builder::addHeader));
        try (Response execute = okHttpClient
                .newCall(builder.build())
                .execute()) {
            int code = execute.code();
            @SuppressWarnings("ConstantConditions")
            byte[] bytes = forceEmptyContent ? new byte[0] : execute.body().bytes();
            final Map<String, List<String>> headers = execute.headers().toMultimap();
            long finish = times.now();
            return new CoreHttpResponseDefaultImpl(finish - start, code, bytes, headers);
        }
    }

    private static class RetryException extends RuntimeException {
        public RetryException(CoreHttpResponse resp) {
            super(resp.code() + " " + resp.newAsString());
        }
    }

    private static class CoreHttpResponseDefaultImpl implements CoreHttpResponse {
        private final long durationMs;
        private final int code;
        private final byte[] bytes;
        private final Set<String> headerKeysOriginal;
        private final Map<String, List<String>> headers;
        private volatile String cachedString;

        public CoreHttpResponseDefaultImpl(long durationMs, int code, byte[] bytes, Map<String, List<String>> headers) {
            this.durationMs = durationMs;
            this.code = code;
            this.bytes = bytes;
            this.headerKeysOriginal = Collections.unmodifiableSet(new HashSet<>(headers.keySet()));
            this.headers = headers.entrySet().stream().map($ -> X.x($.getKey().toLowerCase(), $.getValue())).collect(Cc.toMX2());
            cachedString = null;
        }

        @Override
        public long durationMs() {
            return durationMs;
        }

        @Override
        public int code() {
            return code;
        }

        @Override
        public String cacheAsString() {
            if (cachedString == null) {
                synchronized (this) {
                    if (cachedString == null) {
                        cachedString = newAsString();
                    }
                }
            }
            return cachedString;
        }

        @Override
        public String newAsString() {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        @Override
        public byte[] getAsBytes() {
            return bytes;
        }

        @Override
        public boolean isBusinessProblem() {
            return getHeader(JskProblem.PROBLEM_SIGN).filter($ -> Fu.equal($, "+")).isPresent();
        }

        @Override
        public O<String> getHeader(String header) {
            return getHeaders(header.toLowerCase()).flatMap(Cc::first);
        }

        @Override
        public O<List<String>> getHeaders(String header) {
            return O.ofNull(headers.get(header.toLowerCase()));
        }

        @Override
        public Set<String> getHeaders() {
            return headerKeysOriginal;
        }
    }
}
