package sk.spring.config.params;

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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import sk.spring.plugins.PropertyMeta;
import sk.spring.plugins.PropertyMetaService;
import sk.spring.utils.Profile;
import sk.utils.statics.St;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

@Configuration
public class BaseSpringParamConfig {
    /**
     * NO AUTOWIRED HERE!!!
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer ppc() {
        final PropertyMeta properties = PropertyMetaService.getPropertyNames().get();
        final String profile = Profile.getCurrentProfile().orElseGet(() -> "default");

        final String[] commonProperties = properties.getConf().stream()
                .filter($ -> St.count($.replace(properties.getPropertyPath(), ""), "/") == 0)
                .toArray(String[]::new);

        final String[] profileProperties = properties.getConf().stream()
                .filter($ -> $.contains("/" + profile + "/"))
                .toArray(String[]::new);

        PropertySourcesPlaceholderConfigurer ppc = new PropertySourcesPlaceholderConfigurer();

        ppc.setLocations(concat(of(commonProperties), of(profileProperties))
                .map(ClassPathResource::new)
                .toArray(ClassPathResource[]::new));
        ppc.setEnvironment(new StandardEnvironment());
        ppc.setLocalOverride(false);
        return ppc;
    }
}
