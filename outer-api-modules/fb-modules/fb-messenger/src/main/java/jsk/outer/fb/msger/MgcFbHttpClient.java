package jsk.outer.fb.msger;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import com.github.messenger4j.spi.MessengerHttpClient;
import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import sk.services.http.IHttp;
import sk.services.http.model.CoreHttpResponse;
import sk.utils.statics.Cc;
import sk.utils.statics.Ma;

import java.io.IOException;

@AllArgsConstructor
@NoArgsConstructor
public class MgcFbHttpClient implements MessengerHttpClient {
    @Inject IHttp http;

    @Override
    public HttpResponse execute(HttpMethod httpMethod, String url, String jsonBody) throws IOException {
        final CoreHttpResponse response = (switch (httpMethod) {
            case GET -> http.get(url);
            case POST -> http.postBody(url)
                    .body(jsonBody)
                    .headers(Cc.m("Content-type", "application/json; charset=utf-8"));
            default -> throw new IOException("Unsupported method:" + httpMethod);
        }).goResponseAndThrow();
        return switch (Ma.inside(response.code(), 200, 299) ? 1 : 0) {
            case 1 -> new HttpResponse(response.code(), response.newAsString());
            case 0 -> throw new IOException(
                    "Bad code:%d body:%s".formatted(response.code(), response.newAsString()));
            default -> throw new IllegalStateException("Unexpected value: " + (Ma.inside(response.code(), 200, 299) ? 1 : 0));
        };
    }
}
