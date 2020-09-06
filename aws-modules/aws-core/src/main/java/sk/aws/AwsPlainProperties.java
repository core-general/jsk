package sk.aws;

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

import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;

import java.net.URI;

public interface AwsPlainProperties extends AwsProperties {
    @Override
    default OneOf<URI, Region> getAddress() throws IllegalArgumentException {
        String rawAddr = getAddressRaw().toLowerCase().trim();
        if (rawAddr.contains(".") || rawAddr.contains(":") || "localhost".equals(rawAddr)) {
            if (!(rawAddr.startsWith("http://") || rawAddr.startsWith("https://"))) {
                rawAddr = "http://" + rawAddr;
            }
            return OneOf.left(URI.create(rawAddr));
        } else if (rawAddr.contains("-") || rawAddr.contains("_")) {
            String finalRawAddr = rawAddr;
            final O<Region> regionO = Cc.find(Region.regions(), region -> {
                final String o1 = region.id().toLowerCase();
                return Fu.equal(o1, finalRawAddr) || Fu.equal(o1.replace("-", "_"), finalRawAddr);
            });
            if (regionO.isPresent()) {
                return OneOf.right(regionO.get());
            }
        }

        throw new IllegalArgumentException("Address: '" + rawAddr + "' can't be processed for AwsPlainProperties");
    }

    @Override
    default AwsCredentials getCredentials() {
        return AwsBasicCredentials.create(getAccessKey(), getSecret());
    }

    /**
     * either AWS region or url or localhost or if contains "." and no http(s):// before - add it.
     * Examples:
     * eu-west-1
     * EU-WEST-1
     * EU_WEST_1
     * <p>
     * localhost -> http://localhost
     * localhost:8080 -> http://localhost:8080
     * <p>
     * https://sfo2.digitaloceanspaces.com -> https://sfo2.digitaloceanspaces.com
     * digitaloceanspaces.com -> http://digitaloceanspaces.com
     *
     * @return
     */
    public String getAddressRaw();

    public String getAccessKey();

    public String getSecret();
}
