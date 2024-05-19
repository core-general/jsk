package sk.test.land.testcontainers.localstack;

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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.aws.AwsUtilityHelper;
import sk.aws.AwsWithChangedPort;
import sk.services.ICoreServices;
import sk.utils.statics.Io;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class JskLandLocalstackConfig {
    @Bean
    JskLandLocalstack JskLandLocalstack(AwsWithChangedPort acp, AwsUtilityHelper awh, ICoreServices core) {
        return new JskLandLocalstack(acp, "localstack/localstack:3.4.0", awh, core);
    }

    @Bean
    AwsWithChangedPort AwsWithChangedPort() {
        AtomicInteger ai = new AtomicInteger();
        return () -> Io.getFreePort(ai).get();
    }
}
