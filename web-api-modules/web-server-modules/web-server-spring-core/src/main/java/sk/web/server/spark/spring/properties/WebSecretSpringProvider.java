package sk.web.server.spark.spring.properties;

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

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.web.auth.WebSecretProvider;

import java.util.Collections;
import java.util.Set;

public class WebSecretSpringProvider implements WebSecretProvider {
    @Value("${spark_secret:#{null}}")
    @Getter(AccessLevel.PRIVATE)
    private String sparkSecret;
    @Value("${spark_secret_list:#{null}}")
    @Getter(AccessLevel.PRIVATE)
    private String sparkSecretList;

    private O<Set<String>> sparkSecretReady;

    @PostConstruct
    void init() {
        Set<String> toRet = Cc.s();

        if (sparkSecret != null) {
            toRet.add(sparkSecret.trim());
        }
        if (sparkSecretList != null) {
            Cc.stream(sparkSecretList.split("#")).map($ -> $.trim()).forEach(toRet::add);
        }
        sparkSecretReady = toRet.size() == 0 ? O.empty() : O.of(Collections.unmodifiableSet(toRet));
    }

    @Override
    public Set<String> getPossibleSecrets() {
        return sparkSecretReady.orElse(Cc.sEmpty());
    }
}
