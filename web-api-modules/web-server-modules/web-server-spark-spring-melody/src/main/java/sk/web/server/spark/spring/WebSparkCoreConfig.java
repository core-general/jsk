package sk.web.server.spark.spring;

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
import org.springframework.context.annotation.ImportResource;
import sk.services.profile.IAppProfile;
import sk.utils.functional.O;
import sk.web.auth.WebAuthServerWithStaticSecrets;
import sk.web.auth.WebSecretProvider;
import sk.web.exceptions.IWebExcept;
import sk.web.renders.inst.*;
import sk.web.server.context.WebContextHolder;
import sk.web.server.context.WebRequestResponseInfo;
import sk.web.server.context.WebRequestResponseInfoImpl;
import sk.web.server.filters.standard.*;
import sk.web.server.params.WebAdditionalParams;
import sk.web.server.params.WebDdosParams;
import sk.web.server.params.WebExceptionParams;
import sk.web.server.params.WebServerParams;
import sk.web.server.spark.WebJettyServerStarter;
import sk.web.server.spark.context.WebJettyContextConsumer;
import sk.web.server.spark.context.WebJettyContextConsumer4Melody;
import sk.web.server.spark.context.WebJettyContextConsumer4Spark;
import sk.web.server.spark.melodycollector.WebMelodyParams;
import sk.web.server.spark.melodycollector.WebMelodySpringParams;
import sk.web.server.spark.spring.properties.WebSecretSpringProvider;
import sk.web.server.spark.spring.properties.WebServerSpringParams;

import java.time.Duration;
import java.util.List;


@Configuration
@ImportResource("classpath:net/bull/javamelody/monitoring-spring.xml")
public class WebSparkCoreConfig {
    //region MAIN
    @Bean
    public WebJettyServerStarter WebJettyServerStarter(WebServerParams params, List<WebJettyContextConsumer> contextConsumers) {
        return new WebJettyServerStarter(params, contextConsumers);
    }

    @Bean
    public WebAuthServerWithStaticSecrets WebAuthServerWithStaticSecrets() {
        return new WebAuthServerWithStaticSecrets();
    }

    @Bean
    public WebContextHolder WebContextHolder() { return new WebContextHolder(); }

    @Bean
    public WebJettyContextConsumer4Spark WebJettyContextConsumer4Spark() {
        return new WebJettyContextConsumer4Spark();
    }

    @Bean
    public WebJettyContextConsumer4Melody WebJettyContextConsumer4Melody() {
        return new WebJettyContextConsumer4Melody();
    }

    @Bean
    public IWebExcept IWebExcept(WebJsonRender render) {
        return () -> render;
    }

    @Bean
    public WebServerNodeInfo WebServerNodeInfo() {
        return new WebServerNodeInfo();
    }

    @Bean
    public WebRequestResponseInfo WebRequestResponseInfoImpl() {
        return new WebRequestResponseInfoImpl();
    }
    //endregion

    //region Parameters
    @Bean
    public WebDdosParams WebDdosParams(IAppProfile profile) {
        return new WebDdosParams() {
            @Override
            public Duration getUserInCourtPeriod() {
                return Duration.ofSeconds(5);
            }

            @Override
            public int getUserRequestsAllowedInCourt() {
                return 10;
            }

            @Override
            public Duration getUserInJailTime() {
                return Duration.ofMinutes(5);
            }

            @Override
            public boolean isDdosCourtEnabled() {
                return !profile.getProfile().isForDefaultTesting();
            }
        };
    }

    @Bean
    public WebAdditionalParams WebAdditionalParams() {
        return (w) -> O.empty();
    }

    @Bean
    public WebSecretProvider WebSecretProvider() {
        return new WebSecretSpringProvider();
    }

    @Bean
    public WebMelodyParams WebMelodyParams() {
        return new WebMelodySpringParams();
    }

    @Bean
    public WebServerParams WebServerParams() {
        return new WebServerSpringParams();
    }

    @Bean
    public WebExceptionParams WebExceptionParams() {
        return new WebExceptionParams() {
            @Override
            public int getUnhandledJskExceptionHttpCode() {
                return 500;
            }

            @Override
            public int getUnknownExceptionHttpCode() {
                return 500;
            }
        };
    }
    //endregion

    //region Filters
    @Bean
    public WebAuthFilter WebAuthFilter() { return new WebAuthFilter(); }

    @Bean
    public WebDdosFilter WebDdosFilter() { return new WebDdosFilter(); }

    @Bean
    public WebDefaultHeadersFilter WebDefaultHeadersFilter() { return new WebDefaultHeadersFilter(); }

    @Bean
    public WebIdempotenceFilter WebIdempotenceFilter() { return new WebIdempotenceFilter(); }

    @Bean
    public WebRequestLoggingFilter WebRequestLoggingFilter() { return new WebRequestLoggingFilter(); }

    @Bean
    public WebShutdownFilter WebShutdownFilter() { return new WebShutdownFilter(); }

    @Bean
    public WebExceptionFilter WebExceptionFilter() { return new WebExceptionFilter(); }

    @Bean
    public WebRenderFilter WebRenderFilter() { return new WebRenderFilter(); }

    @Bean
    public WebContextExplicatorFilter WebContextExplicatorFilter() { return new WebContextExplicatorFilter(); }
    //endregion

    //region Renders
    @Bean
    public WebB64Render WebB64Render() { return new WebB64Render(); }

    @Bean
    public WebJsonPrettyRender WebJsonPrettyRender() { return new WebJsonPrettyRender(); }

    @Bean
    public WebJsonRender WebJsonRender() { return new WebJsonRender(); }

    @Bean
    public WebRawByteRenderZipped WebRawByteRenderZipped() { return new WebRawByteRenderZipped(); }

    @Bean
    public WebRawStringRender WebRawStringRender() { return new WebRawStringRender(); }
    //endregion
}
