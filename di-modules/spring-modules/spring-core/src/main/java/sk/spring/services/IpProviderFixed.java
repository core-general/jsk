package sk.spring.services;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sk.services.nodeinfo.IIpProvider;
import sk.spring.config.params.IpProviderParams;
import sk.utils.functional.O;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class IpProviderFixed implements IIpProvider {
    @Inject
    IpProviderParams params;

    @Override
    public O<String> getMyIp() {
        return O.ofNull(params.getMyPublicIp());
    }

    @Override
    public O<String> getMyPrivateIp() {
        return O.ofNull(params.getMyPrivateIp());
    }
}
