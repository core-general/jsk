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

public class WebMelodyCollectorSpringParams implements WebMelodyCollectorParams {
    @Getter
    @Value("${melody_collector_on}")
    boolean melodyCollectorOn;

    @Getter
    @Value("${melody_collector_host}")
    String host;

    @Getter
    @Value("${melody_collector_port}")
    int port;

    @Getter
    @Value("${melody_collector_login}")
    String login;

    @Getter
    @Value("${melody_collector_password}")
    String pass;
}
