package sk.outer.api.ios.signin;

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

import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import sk.outer.api.OutSimpleUserInfo;
import sk.outer.api.ios.OutGeneralIosRequester;
import sk.services.http.IHttp;
import sk.services.json.IJson;
import sk.services.time.ITime;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

//https://developer.apple.com/documentation/sign_in_with_apple/sign_in_with_apple_rest_api/authenticating_users_with_sign_in_with_apple
@NoArgsConstructor
public class OutIosSigninRequester extends OutGeneralIosRequester<OutIosSignInTokenResponse> {
    protected @Inject IHttp http;

    public OutIosSigninRequester(IJson json, ITime times, IHttp http) {
        super(json, times);
        this.http = http;
    }

    public O<OutSimpleUserInfo> validateUser(
            String authCode,

            String clientId,
            String sub,

            byte[] pkcs8PemFile,
            String keyID,
            boolean sandbox) {
        var result = executeRequest(
                pkcs8PemFile,
                "https://appleid.apple.com",
                keyID,
                clientId,
                sandbox,
                token -> http.postForm("https://appleid.apple.com/auth/token")
                        .parameters(Cc.m(
                                "client_id", clientId,
                                "client_secret", token,
                                "grant_type", "authorization_code",
                                "code", authCode
                        )),
                OutIosSignInTokenResponse.class,
                builder -> builder.withSubject(sub)
        );

        return result.collect(
                response -> O.of(new OutSimpleUserInfo(
                        response.getId_token().getSub(),
                        ""
                )),
                e -> O.empty());
    }
}
