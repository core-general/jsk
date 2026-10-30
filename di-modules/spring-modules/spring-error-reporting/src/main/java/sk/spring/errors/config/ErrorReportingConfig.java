package sk.spring.errors.config;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2026 Core General
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
import sk.services.async.IAsync;
import sk.services.http.IHttp;
import sk.services.json.IJson;
import sk.services.time.ITime;
import sk.services.errors.IErrorReportingScope;
import sk.spring.errors.DisabledErrorReporter;
import sk.spring.errors.ErrorEventFactory;
import sk.spring.errors.ErrorReportingLogbackAppender;
import sk.spring.errors.ErrorReportingLogbackAttacher;
import sk.spring.errors.ErrorReportingSettings;
import sk.spring.errors.ILoggedErrorReporter;
import sk.spring.errors.QueueingErrorReporter;
import sk.spring.errors.ThreadErrorReportingScope;

@Configuration(proxyBeanMethods = false)
public class ErrorReportingConfig {
    @Bean
    public ErrorEventFactory errorEventFactory(ErrorReportingSettings settings) {
        return new ErrorEventFactory(settings);
    }

    @Bean
    public ILoggedErrorReporter errorReporter(ErrorReportingSettings settings, ErrorEventFactory events, IHttp http,
            IJson json, IAsync async, ITime time) {
        return settings.enabled()
               ? new QueueingErrorReporter(settings, events, http, json, async, time)
               : new DisabledErrorReporter();
    }

    @Bean
    public IErrorReportingScope errorReportingScope() {
        return new ThreadErrorReportingScope();
    }

    @Bean
    public ErrorReportingLogbackAppender errorReportingLogbackAppender(ILoggedErrorReporter reporter, IErrorReportingScope scopes) {
        return new ErrorReportingLogbackAppender(reporter, scopes);
    }

    @Bean
    public ErrorReportingLogbackAttacher errorReportingLogbackAttacher(ErrorReportingSettings settings,
            ErrorReportingLogbackAppender appender) {
        return new ErrorReportingLogbackAttacher(settings, appender);
    }
}
