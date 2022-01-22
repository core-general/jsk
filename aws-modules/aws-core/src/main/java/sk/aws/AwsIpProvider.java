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

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import sk.services.http.IHttp;
import sk.services.http.model.CoreHttpResponse;
import sk.services.nodeinfo.IIpProvider;
import sk.utils.functional.O;

import javax.inject.Inject;

@Log4j2
@NoArgsConstructor
@AllArgsConstructor
public class AwsIpProvider implements IIpProvider {
    @Inject IHttp http;

    @Override
    public O<String> getMyIp() {
        try {
            final CoreHttpResponse response = http
                    .get("http://169.254.169.254/latest/meta-data/public-ipv4")
                    .goResponseAndThrow();
            if (response.code() != 200) {
                throw new RuntimeException("Can't get my ip by AWS: " + response.code() + " " + response.newAsString());
            }
            return O.of(response.newAsString().trim());
        } catch (Exception e) {
            log.error("", e);
            return O.empty();
        }
    }

    @Override
    public O<String> getMyPrivateIp() {
        try {
            final CoreHttpResponse response = http
                    .get("http://169.254.169.254/latest/meta-data/local-ipv4")
                    .goResponseAndThrow();
            if (response.code() != 200) {
                throw new RuntimeException("Can't get my ip by AWS: " + response.code() + " " + response.newAsString());
            }
            return O.of(response.newAsString().trim());
        } catch (Exception e) {
            log.error("", e);
            return O.empty();
        }
    }
}
