package sk.test.land.core;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2025 Core General
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
import org.springframework.context.annotation.Primary;
import sk.services.async.IAsync;
import sk.services.json.IJson;
import sk.services.land.JskLandLocalPortLoader;
import sk.utils.land.JskLandLoadedToken;
import sk.utils.land.JskWithChangedPort;
import sk.utils.statics.Cc;

import java.util.List;

@Configuration
public class JskLandDefaultConfig {
    @Bean
    JskFullLand JskFullLand(List<JskLand> lands, IAsync async) {
        return new JskFullLand(lands, async);
    }

    @Bean
    @Primary
    JskLandLocalPortLoader JskLandLocalPortLoader(List<JskWithChangedPort> allWithChanedPorts, IJson json) {
        JskLandLocalPortLoader jskLandLocalPortLoader = new JskLandLocalPortLoader(json, true);
        jskLandLocalPortLoader.save(allWithChanedPorts.stream().collect(Cc.toM($ -> $.getType(), $ -> $.getPort())));
        return jskLandLocalPortLoader;
    }

    @Bean
    @Primary
    JskLandLoadedToken JskLandLoadedToken(JskFullLand loadedLand) {
        return new JskLandLoadedToken();
    }
}
