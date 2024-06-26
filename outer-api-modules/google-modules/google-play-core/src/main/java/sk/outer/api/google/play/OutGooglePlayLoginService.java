package sk.outer.api.google.play;

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

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import sk.outer.api.OutSimpleUserInfo;
import sk.services.http.IHttp;
import sk.services.http.model.CoreHttpResponse;
import sk.services.json.IJson;
import sk.services.retry.IRepeat;
import sk.services.time.ITime;
import sk.utils.functional.O;
import sk.utils.statics.Ex;

import java.util.Base64;


@Slf4j
public class OutGooglePlayLoginService {
    @Inject private IJson json;
    @Inject private ITime times;
    @Inject private IRepeat retry;
    @Inject private IHttp http;
    private GoogleIdTokenVerifier verifier;
    private GsonFactory jsonFactory;

    @PostConstruct
    private void init() {
        jsonFactory = new GsonFactory();
        verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setClock(() -> times.now())
                .build();
    }

    public O<OutSimpleUserInfo> getSimpleUserByIdTokenVerification(String idToken) {
        return retry.repeat(() -> {
            if (!verify(idToken)) {
                return O.empty();
            }

            String[] split = idToken.split("\\.");
            String body = split[1];
            GoogleRestUser googleRestUser = json.from(new String(Base64.getDecoder().decode(body)), GoogleRestUser.class);
            return O.of(new OutSimpleUserInfo(googleRestUser.getSub(), googleRestUser.getName()));
        }, O::empty, 3);
    }

    public O<OutSimpleUserInfo> getSimpleUserByGoogleOauth(String accessToken) {
        final String url = "https://www.googleapis.com/oauth2/v2/userinfo?access_token=" + accessToken;
        try {
            final CoreHttpResponse response = http.get(url).goResponseAndThrow();
            final String result = response.newAsString();
            if (response.code() != 200) {
                log.error("Bad status: code=" + response.code() + " response=" + response);
                return O.empty();
            }
            return O.of(json.from(result, OutSimpleUserInfo.class));
        } catch (Exception e) {
            log.error("Can't get " + url, e);
        }

        return O.empty();
    }

    private boolean verify(String idToken) {
        return Ex.toRuntime(() -> GoogleIdToken.parse(jsonFactory, idToken).verify(verifier));
    }

    @Data
    private static class GoogleRestUser {
        String sub;
        String name;
    }
}
