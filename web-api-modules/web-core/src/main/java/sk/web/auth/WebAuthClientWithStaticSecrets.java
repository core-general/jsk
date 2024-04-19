package sk.web.auth;

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

import jakarta.inject.Inject;
import sk.utils.functional.O;

import java.util.Optional;

public class WebAuthClientWithStaticSecrets implements WebAuthClient {
    @Inject Optional<WebSecretProvider> secretProvider;

    @Override
    public O<String> getSecret4Client() {
        try {
            return O.of(secretProvider.map($ -> $.getPossibleSecrets().iterator().next()));
        } catch (Exception e) {
            return O.empty();
        }
    }
}
