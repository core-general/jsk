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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import sk.services.http.model.CoreHttpResponse;
import sk.services.http.model.IHttpConf;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Ex;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public interface IHttp {
    F1<HttpGetBuilder, OneOf<CoreHttpResponse, Exception>> howToGet();

    <T extends HttpPostBuilder<T>> F1<T, OneOf<CoreHttpResponse, Exception>> howToPost();

    <T extends HttpPostBuilder<T>> F1<T, OneOf<CoreHttpResponse, Exception>> howToDelete();

    F1<HttpHeadBuilder, OneOf<CoreHttpResponse, Exception>> howToHead();

    default IHttpConf getConfig() {
        return new IHttpConf(Duration.ofSeconds(10), Duration.ofSeconds(20));
    }

    default HttpHeadBuilder head(String url) {
        return new HttpHeadBuilder(url, howToHead());
    }

    default CoreHttpResponse headResp(String url) {
        return head(url).tryCount(5).goResponse().left();
    }

    default HttpGetBuilder get(String url) {
        return new HttpGetBuilder(url, howToGet());
    }

    default String getS(String url) {
        return get(url).tryCount(5).go().left();
    }

    default byte[] getB(String url) {
        return get(url).tryCount(5).goBytes().left();
    }

    default HttpFormBuilder postForm(String url) {
        return new HttpFormBuilder(url, howToPost());
    }

    default HttpBodyBuilder postBody(String url) {
        return new HttpBodyBuilder(url, howToPost());
    }

    default HttpMultipartBuilder postMulti(String url) {
        return new HttpMultipartBuilder(url, howToPost());
    }

    default String postForm(String url, Map<String, String> params) {
        return postForm(url).parameters(params).go().left();
    }

    default HttpFormBuilder deleteForm(String url) {
        return new HttpFormBuilder(url, howToDelete());
    }

    default HttpBodyBuilder deleteBody(String url) {
        return new HttpBodyBuilder(url, howToDelete());
    }

    default HttpMultipartBuilder deleteMulti(String url) {
        return new HttpMultipartBuilder(url, howToDelete());
    }

    @Accessors(fluent = true, chain = true)
    @Setter
    @Getter
    abstract class HttpBuilder<T extends HttpBuilder> {
        @Setter(AccessLevel.PRIVATE) F1<T, OneOf<CoreHttpResponse, Exception>> requester;
        String url;
        @Nullable String login = null;
        @Nullable String password = null;
        @Nullable Map<String, String> headers = null;
        int tryCount = 1;
        int trySleepMs = 0;
        O<Duration> timeout = O.empty();

        @SneakyThrows
        HttpBuilder(String url) {
            //very simple solution for percent-encoding problem, there exist better but heavier solutions
            this.url = url.replace(" ", "%20");
        }

        public Map<String, String> headers() {
            if (headers == null) {
                headers = new HashMap<>();
            }
            return headers;
        }

        public OneOf<String, Exception> go() {
            return goResponse().flatMap($ -> {
                if ($.code() >= 200 && $.code() <= 299) {
                    return OneOf.left($.newAsString());
                } else {
                    return OneOf.right(new RuntimeException("Error code '" + $.code() + "' " + $.newAsString()));
                }
            }, $ -> OneOf.right($));
        }

        public OneOf<byte[], Exception> goBytes() {
            return goResponse().flatMap($ -> {
                if ($.code() >= 200 && $.code() <= 299) {
                    return OneOf.left($.getAsBytes());
                } else {
                    return OneOf.right(new RuntimeException("Error code '" + $.code() + "' " + $.newAsString()));
                }
            }, $ -> OneOf.right($));
        }

        public OneOf<CoreHttpResponse, Exception> goResponse() {
            return requester.apply(getThis());
        }

        public String goAndThrow() throws RuntimeException {
            return go().collect($ -> $, Ex::thRow);
        }

        public byte[] goBytesAndThrow() throws RuntimeException {
            return goBytes().collect($ -> $, Ex::thRow);
        }

        public CoreHttpResponse goResponseAndThrow() throws RuntimeException {
            return goResponse().collect($ -> $, Ex::thRow);
        }

        abstract T getThis();
    }

    class HttpGetBuilder extends HttpBuilder<HttpGetBuilder> {
        private HttpGetBuilder(String url, F1<HttpGetBuilder, OneOf<CoreHttpResponse, Exception>> requester) {
            super(url);
            this.requester = requester;
        }

        @Override
        HttpGetBuilder getThis() {
            return this;
        }
    }

    class HttpHeadBuilder extends HttpBuilder<HttpHeadBuilder> {
        private HttpHeadBuilder(String url, F1<HttpHeadBuilder, OneOf<CoreHttpResponse, Exception>> requester) {
            super(url);
            this.requester = requester;
        }

        @Override
        HttpHeadBuilder getThis() {
            return this;
        }
    }

    abstract class HttpPostBuilder<T extends HttpBuilder> extends HttpBuilder<T> {
        private HttpPostBuilder(String url, F1<T, OneOf<CoreHttpResponse, Exception>> requester) {
            super(url);
            this.requester = requester;
        }

        public abstract HttpPostType getType();
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    @Getter
    class HttpFormBuilder extends HttpPostBuilder<HttpFormBuilder> {
        @Setter(AccessLevel.PRIVATE) F1<HttpFormBuilder, OneOf<CoreHttpResponse, Exception>> requester;
        @Nullable Map<String, String> parameters = null;

        private HttpFormBuilder(String url, F1<HttpFormBuilder, OneOf<CoreHttpResponse, Exception>> requester) {
            super(url, requester);
        }

        @Override
        public HttpPostType getType() {
            return HttpPostType.FORM;
        }

        @Override
        HttpFormBuilder getThis() {
            return this;
        }

        public Map<String, String> parameters() {
            if (parameters == null) {
                parameters = new HashMap<>();
            }
            return parameters;
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    @Getter
    class HttpMultipartBuilder extends HttpPostBuilder<HttpMultipartBuilder> {
        @Setter(AccessLevel.PRIVATE) F1<HttpMultipartBuilder, OneOf<CoreHttpResponse, Exception>> requester;
        @Nullable Map<String, String> parameters = null;
        @Nullable Map<String, byte[]> rawParameters = null;

        private HttpMultipartBuilder(String url, F1<HttpMultipartBuilder, OneOf<CoreHttpResponse, Exception>> requester) {
            super(url, requester);
        }

        public Map<String, String> parameters() {
            if (parameters == null) {
                parameters = new HashMap<>();
            }
            return parameters;
        }

        public Map<String, byte[]> rawParameters() {
            if (rawParameters == null) {
                rawParameters = new HashMap<>();
            }
            return rawParameters;
        }

        @Override
        public HttpPostType getType() {
            return HttpPostType.MULTIPART;
        }

        @Override
        HttpMultipartBuilder getThis() {
            return this;
        }
    }

    @Accessors(fluent = true, chain = true)
    @Getter
    class HttpBodyBuilder extends HttpPostBuilder<HttpBodyBuilder> {
        @Setter(AccessLevel.PRIVATE) F1<HttpBodyBuilder, OneOf<CoreHttpResponse, Exception>> requester;
        OneOf<String, byte[]> body;

        private HttpBodyBuilder(String url, F1<HttpBodyBuilder, OneOf<CoreHttpResponse, Exception>> requester) {
            super(url, requester);
        }

        public HttpBodyBuilder body(OneOf<String, byte[]> body) {
            this.body = body;
            return this;
        }

        public HttpBodyBuilder body(String body) {
            this.body = OneOf.left(body);
            return this;
        }

        public HttpBodyBuilder body(byte[] body) {
            this.body = OneOf.right(body);
            return this;
        }

        @Override
        public HttpPostType getType() {
            return HttpPostType.BODY;
        }

        @Override
        HttpBodyBuilder getThis() {
            return this;
        }
    }

    enum HttpPostType {
        FORM, MULTIPART, BODY
    }
}
