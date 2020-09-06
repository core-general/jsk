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

import lombok.Data;
import sk.services.profile.IAppProfile;
import sk.services.profile.IAppProfileType;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.Fu;

import javax.annotation.PostConstruct;


@Data
public class AppProfileImpl<T extends IAppProfileType> implements IAppProfile<T> {
    private final Class<T> cls;
    private T value;

    @PostConstruct
    public void init() {
        if (!cls.isEnum()) {
            Ex.thRow(cls + " is not enum");
        }
        String profile = O
                .ofNullable(System.getProperty("spring.profiles.active"))
                .or(() -> Cc.stream(cls.getEnumConstants())
                        .filter(IAppProfileType::isForDefaultTesting)
                        .map(IAppProfileType::name)
                        .findAny())
                .orElseThrow(() -> new RuntimeException("Unknown profile"));

        value = Cc.stream(cls.getEnumConstants())
                .filter($ -> Fu.equal(profile.toLowerCase(), $.toLowerCase()))
                .findAny()
                .orElseGet(() -> Ex.thRow(profile + " is unknown"));
    }

    @Override
    public T getProfile() {
        return value;
    }
}
