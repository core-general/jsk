package sk.spring.services;

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

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.Data;
import org.springframework.core.env.ConfigurableEnvironment;
import sk.services.profile.IAppProfile;
import sk.services.profile.IAppProfileType;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.Fu;

import java.util.Arrays;


@Data
public class AppProfileImpl<T extends IAppProfileType> implements IAppProfile<T> {
    private final Class<T> cls;
    private T value;

    @Inject ConfigurableEnvironment env;

    @PostConstruct
    public void init() throws RuntimeException {
        if (!cls.isEnum()) {
            Ex.thRow(cls + " is not enum");
        }
        String[] activeProfiles = env.getActiveProfiles();
        if (activeProfiles.length != 1) {
            throw new RuntimeException(
                    "Profile should be explicitly set and it should be only one with 'spring.profiles.active' (or with " +
                    "ConfigurableEnvironment redefinition). Available profiles:%s"
                            .formatted(Arrays.toString(cls.getEnumConstants())));
        }

        String profile = activeProfiles[0];

        value = Cc.stream(cls.getEnumConstants())
                .filter($ -> Fu.equalIgnoreCase(profile, $.name()))
                .findFirst()
                .orElseGet(() -> Ex.thRow(profile + " is unknown"));
    }

    @Override
    public T getProfile() {
        return value;
    }
}
