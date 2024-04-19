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

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import sk.exceptions.JskProblem;
import sk.services.ICoreServices;
import sk.services.async.IAsync;
import sk.services.bytes.IBytes;
import sk.services.http.model.CoreHttpResponse;
import sk.services.ids.IIds;
import sk.services.retry.IRepeat;
import sk.services.time.ITime;
import sk.utils.functional.F1;
import sk.utils.functional.F1E;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;
import sk.utils.statics.Io;
import sk.utils.tuples.X;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpClient.Version.HTTP_2;
import static java.net.http.HttpRequest.*;
import static java.net.http.HttpResponse.BodyHandlers;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static sk.utils.functional.O.ofNullable;
import static sk.utils.statics.Ex.thRow;

@NoArgsConstructor
@SuppressWarnings("unused")
public class HttpImpl implements IHttp {
    public static final byte[] EMPTY_BYTES = new byte[0];
    protected @Inject IRepeat retry;
    protected @Inject ITime times;
    protected @Inject IAsync async;
    protected @Inject IIds ids;
    protected @Inject IBytes ibytes;

    protected HttpClient httpClient;

    public HttpImpl(ICoreServices core) {
        this.retry = core.repeat();
        this.times = core.times();
        this.async = core.async();
        this.ids = core.ids();
        this.ibytes = core.bytes();
        init();
    }

    public HttpImpl(IRepeat retry, ITime times, IAsync async, IIds ids, IBytes ibytes) {
        this.retry = retry;
        this.times = times;
        this.async = async;
        this.ids = ids;
        this.ibytes = ibytes;
        init();
    }

    @PostConstruct
    protected final HttpImpl init() {
        httpClient = prepareBuilder().build();
        return this;
    }

    protected HttpClient.Builder prepareBuilder() {
        return HttpClient.newBuilder()
                .version(getConfig().forceHttp2() ? HTTP_2 : HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(getConfig().getConnectTimeout());
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
        final Builder bld = newBuilder();
        return execute(postBuilder, bld
                .uri(URI.create(postBuilder.url()))
                .POST(definePostRequestBody(postBuilder, bld)), false);
    }

    private <T extends HttpPostBuilder<T>> CoreHttpResponse executeSingleDelete(T postBuilder) throws IOException {
        final Builder bld = newBuilder();
        return execute(postBuilder, bld
                .uri(URI.create(postBuilder.url()))
                .method("DELETE", definePostRequestBody(postBuilder, bld)), false);
    }

    private CoreHttpResponse executeSingleGet(HttpGetBuilder getBuilder) throws IOException {
        final Builder builder = newBuilder()
                .uri(URI.create(getBuilder.url()))
                .GET();

        return execute(getBuilder, builder, false);
    }

    private CoreHttpResponse executeSingleHead(HttpHeadBuilder headBuilder) throws IOException {
        final Builder builder = newBuilder()
                .uri(URI.create(headBuilder.url()))
                .method("HEAD", BodyPublishers.noBody());

        return execute(headBuilder, builder, true);
    }

    private <T extends HttpPostBuilder<T>> BodyPublisher definePostRequestBody(T pb, Builder builder) {
        switch (pb.getType()) {
            case BODY:
                return ((HttpBodyBuilder) pb).body().collect(
                        str -> BodyPublishers.ofString(str, UTF_8),
                        bytes -> BodyPublishers.ofByteArray(bytes)
                );
            case FORM:
                builder.header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
                final String string = Cc.join("&", ((HttpFormBuilder) pb).parameters().entrySet().stream()
                        .map($ -> URLEncoder.encode($.getKey(), UTF_8) + "=" + URLEncoder.encode($.getValue(), UTF_8)));
                return BodyPublishers.ofString(string, UTF_8);
            case MULTIPART:
                String boundary = "----" + ids.shortIdS();
                builder.header("Content-Type", "multipart/form-data; boundary=" + boundary);
                List<byte[]> lines = Cc.l();

                byte[] separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=").getBytes(UTF_8);


                for (Map.Entry<String, String> entry : ((HttpMultipartBuilder) pb).parameters().entrySet()) {
                    lines.add(separator);
                    lines.add((String.format("\"%s\"\r\n\r\n%s\r\n", entry.getKey(), entry.getValue()))
                            .getBytes(StandardCharsets.UTF_8));
                }

                for (Map.Entry<String, byte[]> entry : ((HttpMultipartBuilder) pb).rawParameters().entrySet()) {
                    lines.add(separator);

                    lines.add((String.format("\"%s\"\r\nContent-Type: application/octet-stream\r\n\r\n", entry.getKey()))
                            .getBytes(StandardCharsets.UTF_8));
                    lines.add(entry.getValue());
                    lines.add("\r\n".getBytes(StandardCharsets.UTF_8));
                }

                lines.add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));

                return BodyPublishers.ofByteArrays(lines);
        }
        return thRow(pb.getType() + " unknown");
    }

    private <T extends HttpBuilder<T>> CoreHttpResponse execute(HttpBuilder<T> xBuilder, Builder builder,
            boolean forceEmptyContent)
            throws IOException {
        long start = times.now();
        if (xBuilder.login() != null && xBuilder.password() != null) {
            builder.header("Authorization", Credentials.basic(xBuilder.login(), xBuilder.password(), ibytes));
        }
        builder.timeout(xBuilder.timeout().orElseGet(() -> getConfig().getReadTimeout()));

        ofNullable(xBuilder.headers()).ifPresent($ -> $.forEach(builder::header));

        //region Gzip or other compression
        boolean encodeAsGzip = false;
        if (!xBuilder.headers().containsKey("Accept-Encoding")) {
            builder.header("Accept-Encoding", "gzip");
            encodeAsGzip = true;
        }
        if (Fu.equal(xBuilder.headers().get("Accept-Encoding"), "gzip")) {
            encodeAsGzip = true;
        }
        //endregion

        try {
            HttpResponse<?> response = null;
            byte[] bytes = EMPTY_BYTES;
            if (forceEmptyContent) {
                response = httpClient.send(builder.build(), BodyHandlers.discarding());
            } else {
                final HttpResponse<byte[]> resp = httpClient.send(builder.build(), BodyHandlers.ofByteArray());
                bytes = decodeData(resp, encodeAsGzip);
                response = resp;
            }

            int code = response.statusCode();
            final Map<String, List<String>> headers = response.headers().map();
            long finish = times.now();
            return new CoreHttpResponseDefaultImpl(ibytes, finish - start, code, bytes, headers);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private byte[] decodeData(HttpResponse<byte[]> resp, boolean waitForGzip) {
        byte[] bytes = resp.body();
        if (waitForGzip && resp.headers().allValues("Content-Encoding").contains("gzip")) {
            try (var stream = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                bytes = Io.streamToBytes(stream);
            } catch (IOException e) {}
        }
        return bytes;
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
        private final IBytes ibytes;
        private volatile String cachedString;

        public CoreHttpResponseDefaultImpl(IBytes ibytes, long durationMs, int code, byte[] bytes,
                Map<String, List<String>> headers) {
            this.durationMs = durationMs;
            this.code = code;
            this.bytes = bytes;
            this.ibytes = ibytes;
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
            return new String(bytes, UTF_8);
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

        @Override
        public O<EtagAndSize> calcEtagAndSize() {
            return getEtagAndSize().or(
                    () -> {
                        final byte[] data = getAsBytes();
                        try {
                            return O.of(ibytes.calcEtagAndSize(data));
                        } catch (Exception e) {
                            return O.empty();
                        }
                    }
            );
        }
    }

    private static class Credentials {
        private Credentials() {
        }

        /** Returns an auth credential for the Basic scheme. */
        public static String basic(String username, String password, IBytes bytes) {
            return basic(username, password, ISO_8859_1, bytes);
        }

        public static String basic(String username, String password, Charset charset, IBytes bytes) {
            String usernameAndPassword = username + ":" + password;
            final String encoded = bytes.enc64(usernameAndPassword.getBytes(charset));
            return "Basic " + encoded;
        }
    }
}
