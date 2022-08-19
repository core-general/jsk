package sk.services.http.model;

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

import sk.services.http.EtagAndSize;
import sk.utils.functional.O;
import sk.utils.statics.Ma;
import sk.utils.statics.St;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public interface CoreHttpResponse {
    long durationMs();

    int code();

    String cacheAsString();

    String newAsString();

    byte[] getAsBytes();

    boolean isBusinessProblem();

    public O<String> getHeader(String header);

    public O<List<String>> getHeaders(String header);

    public Set<String> getHeaders();

    public default O<EtagAndSize> getEtagAndSize() {
        return O.allNotNull(getHeader("etag"), getHeader("content-length"),
                (etag, length) -> O.of(new EtagAndSize(etag, Ma.pl(length))));
    }

    public default O<EtagAndSize> calcEtagAndSize() {
        return getEtagAndSize().or(
                () -> {
                    final byte[] data = getAsBytes();
                    try {
                        return O.ofNull(
                                new EtagAndSize(St.bytesToHex(MessageDigest.getInstance("MD5").digest(data)), data.length));
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                        return O.empty();
                    }
                }
        );
    }
}
