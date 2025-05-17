package sk.aws;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2024 Core General
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

import sk.utils.functional.OneOf;
import sk.utils.land.JskWithChangedPort;
import sk.utils.land.JskWithChangedPortType;
import sk.utils.statics.Ex;
import sk.utils.statics.Io;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.net.URISyntaxException;

public interface AwsWithChangedPort extends JskWithChangedPort {
    @Override
    default JskWithChangedPortType getType() {
        return AwsPortType.AWS_PORT_TYPE;
    }

    public default OneOf<URI, Region> getAddress(AwsProperties actual) {
        OneOf<URI, Region> addr = actual.getAddress();
        return addr.map(uri -> {
            try {
                return new URI(Io.changePortForUrl(uri.toString(), getPort()));
            } catch (URISyntaxException e) {
                return Ex.thRow(e);
            }
        }, r -> r);
    }
}
