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

import sk.outer.api.OutSimpleUserInfo;
import sk.outer.api.ios.OutGeneralIosRequester;
import sk.services.http.IHttp;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import javax.inject.Inject;

public class OutIosSigninRequester extends OutGeneralIosRequester<OutIosSignInTokenResponse> {
    @Inject IHttp http;

    public O<OutSimpleUserInfo> validateUser(
            String authCode,

            String clientId,

            byte[] pkcs8PemFile,
            String issuerId,
            String keyID,
            boolean sandbox) {
        var result = executeRequest(
                pkcs8PemFile,
                issuerId,
                keyID,
                "https://appleid.apple.com",
                sandbox,
                token -> http.postForm("https://appleid.apple.com/auth/token")
                        .parameters(Cc.m(
                                "client_id", clientId,
                                "client_secret", token,
                                "grant_type", "authorization_code",
                                "code", authCode
                        )),
                OutIosSignInTokenResponse.class,
                builder -> builder.withSubject(clientId)
        );

        return result.collect(
                response -> O.of(new OutSimpleUserInfo(
                        response.getId_token().getSub(),
                        ""
                )),
                e -> O.empty());
    }
}