package sk.web.server.spark.melodycollector;

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

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import sk.utils.functional.O;

public class WebMelodySpringParams implements WebMelodyParams {
    @Getter
    @Value("${melody_app_name}")
    String appName;

    @Getter
    @Value("${melody_enabled}")
    private boolean melodyEnabled;

    @Value("${melody_path:#{null}}")
    private String melodyPath;

    @Getter
    @Value("${melody_login:#{null}}")
    private String login;

    @Getter
    @Value("${melody_password:#{null}}")
    private String pass;

    @Override
    public O<String> getMelodyPathSuffix() {
        return O.ofNull(melodyPath);
    }
}
