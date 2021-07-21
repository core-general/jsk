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

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import sk.utils.functional.O;
import sk.web.server.params.WebServerParams;

public class WebServerSpringParams implements WebServerParams {
    @Getter
    @Value("${web_server_port}")
    private int port;

    @Getter
    @Value("${web_server_formlimit:1000000}")
    private long formLimit;

    @Value("${web_server_static_files_location:#{null}}")
    private String staticFilesLocation;

    @Value("${web_server_idle_timeout:#{null}}")
    private Long idleTimeout;

    @Value("${web_server_shutdown_wait:#{null}}")
    private Long shutdownWait;

    @Value("${web_server_token_timeout_sec:#{null}}")
    private Integer tokenTimeoutSec;

    @Value("${web_server_token_in_cookies:#{false}}")
    @Getter
    private boolean useCookiesForToken;

    @Override
    public O<Long> getIdleTimeout() {
        return O.ofNull(idleTimeout);
    }

    @Override
    public O<Long> getShutdownWait() {
        return O.ofNull(shutdownWait);
    }

    @Override
    public O<String> getStaticFilesLocation() {
        return O.ofNull(staticFilesLocation);
    }

    @Override
    public O<Integer> getTokenTimeoutSec() {
        return O.ofNull(tokenTimeoutSec);
    }
}
