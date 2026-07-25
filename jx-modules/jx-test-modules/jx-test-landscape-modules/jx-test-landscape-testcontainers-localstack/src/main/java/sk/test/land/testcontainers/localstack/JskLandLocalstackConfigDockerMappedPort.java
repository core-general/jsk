package sk.test.land.testcontainers.localstack;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2026 Core General
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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import sk.aws.AwsUtilityHelper;
import sk.aws.AwsWithChangedPort;
import sk.services.ICoreServices;
import sk.test.land.core.JskLandDefaultConfig;

@Configuration
@Import(JskLandDefaultConfig.class)
public class JskLandLocalstackConfigDockerMappedPort {
    @Bean
    JskLandLocalstack JskLandLocalstack(AwsUtilityHelper awh, ICoreServices core) {
        return new JskLandLocalstack("localstack/localstack:3.4.0", awh, core);
    }

    @Bean
    @Primary
    AwsWithChangedPort AwsWithChangedPort(JskLandLocalstack localstack) {
        return localstack::getOutsidePort;
    }
}
