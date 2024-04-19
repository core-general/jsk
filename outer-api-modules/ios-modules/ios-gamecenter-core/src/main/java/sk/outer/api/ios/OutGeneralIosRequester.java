package sk.outer.api.ios;

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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import sk.services.http.IHttp;
import sk.services.http.model.CoreHttpResponse;
import sk.services.json.IJson;
import sk.services.time.ITime;
import sk.utils.functional.F1;
import sk.utils.functional.OneOf;
import sk.utils.functional.Op1;
import sk.utils.statics.Cc;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
public abstract class OutGeneralIosRequester<T> {

    protected @Inject IJson json;
    protected @Inject ITime times;

    protected OneOf<T, Exception> executeRequest(
            byte[] pkcs8PemFile,
            String issuerId,
            String keyID,
            String audience,
            boolean sandbox,
            F1<String, IHttp.HttpBuilder<?>> tokenToHttpRequest,
            Class<T> responseClass,
            Op1<JWTCreator.Builder> prepareJwt) {

        try {
            PrivateKey privateKey = generatePrivateKey(pkcs8PemFile);

            String token = generateToken(issuerId, keyID, privateKey, audience, prepareJwt);

            //System.out.println(
            //        Cc.join("\n", Cc.l(json.beautify(new String(bytes.dec64(token.split("\\.")[0]), StandardCharsets.UTF_8)),
            //                json.beautify(new String(bytes.dec64(token.split("\\.")[1]), StandardCharsets.UTF_8)))));

            final CoreHttpResponse response = tokenToHttpRequest.apply(token).goResponseAndThrow();

            if (response.code() == 200) {
                final String data = response.newAsString();
                final T from = json.from(data, responseClass);
                return OneOf.left(from);
            } else if (!sandbox && response.code() == 404) {
                return executeRequest(pkcs8PemFile, issuerId, keyID, audience, true, tokenToHttpRequest, responseClass,
                        prepareJwt);
            } else {
                return OneOf.right(new RuntimeException("Bad code " + response.code() + " data: " + response.newAsString()));
            }
        } catch (RuntimeException e) {
            return OneOf.right(e);
        }
    }

    private String generateToken(String issuerID, String keyID, PrivateKey privateKey, String audience,
            Op1<JWTCreator.Builder> prepareJwt) {
        return prepareJwt.apply(
                        JWT.create()
                                .withIssuedAt(new Date(times.toMilli(times.nowZ())))
                                .withExpiresAt(new Date(times.toMilli(times.nowZ().plus(20, ChronoUnit.MINUTES))))
                                .withIssuer(issuerID)
                                .withAudience(audience)
                                .withKeyId(keyID)
                )
                .sign(Algorithm.ECDSA256(null, (ECPrivateKey) privateKey));
    }

    private PrivateKey generatePrivateKey(byte[] keyData) {
        return generateKey(new PKCS8EncodedKeySpec(decodePEM(new String(keyData, StandardCharsets.UTF_8))));
    }

    private PrivateKey generateKey(PKCS8EncodedKeySpec keySpec) {
        try {
            return KeyFactory.getInstance("EC").generatePrivate(keySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Problem with EC algorithm!", e);
        }
    }

    private byte[] decodePEM(String pem) {
        var encoded = Cc.stream(pem.split("\n"))
                .filter($ -> !$.startsWith("-"))
                .collect(Collectors.joining(""));

        byte[] decoded = Base64.getMimeDecoder().decode(encoded);
        return decoded;
    }
}
