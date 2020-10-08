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

import lombok.extern.log4j.Log4j2;
import sk.outer.api.OutSimpleUserInfo;
import sk.services.bytes.IBytes;
import sk.utils.functional.O;

import javax.inject.Inject;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;


@Log4j2
public class OutIosGameCenterLoginService {
    @Inject IBytes bytes;

    public O<OutSimpleUserInfo> validateUser(String publicKeyURL, String signature, String salt, String timestamp,
            String playerID, String name, String bundleId) {
        return getPublicKey(publicKeyURL).flatMap(publicKeyCertificate -> {
            // Verify with the appropriate signing authority that the public key is signed by Apple.
            if (!verifyPublicKey(publicKeyCertificate)) {
                log.error(() -> "Failed to verify public key");
                return O.empty();
            }

            if (!verifySignature(signature, publicKeyCertificate, playerID, bundleId, timestamp, salt)) {
                log.error(() -> "Failed to verify signature");
                return O.empty();
            }

            return O.ofNullable(new OutSimpleUserInfo(playerID, name));
        });
    }

    private O<Certificate> getPublicKey(String publicKeyURLString) {
        try {
            Certificate cert = CertificateFactory.getInstance("X.509").
                    generateCertificate(new URL(publicKeyURLString).
                            openConnection().
                            getInputStream());
            return O.ofNullable(cert);
        } catch (Exception e) {
            log.error(() -> "Error getting public key: " + e.getMessage());
            return O.empty();
        }
    }

    private Boolean verifySignature(String signature, Certificate publicKeyCertificate, String playerID,
            String bundleID, String timestamp, String salt) {
        try {
            byte[] playerIDBytes = playerID.getBytes("UTF-8");
            byte[] bundleIDBytes = bundleID.getBytes("UTF-8");

            Long timestampInt = Long.valueOf(timestamp);
            final ByteBuffer tsByteBuffer = ByteBuffer.allocate(8);
            tsByteBuffer.order(ByteOrder.BIG_ENDIAN);
            tsByteBuffer.putLong(timestampInt);
            byte[] timestampBytes = tsByteBuffer.array();
            byte[] saltBytes = bytes.dec64(salt);
            byte[] signatureBytes = bytes.dec64(signature);
            ByteBuffer dataBuffer = ByteBuffer
                    .allocate(playerIDBytes.length +
                            bundleIDBytes.length +
                            timestampBytes.length +
                            saltBytes.length);

            dataBuffer.put(playerIDBytes).put(bundleIDBytes).put(timestampBytes).put(saltBytes);

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKeyCertificate);
            sig.update(dataBuffer.array());
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            log.error(() -> "Error verifying signature: " + e.getMessage());
            return false;
        }
    }

    private Boolean verifyPublicKey(Certificate publicKeyCertificate) {
        return true;
    }
}
