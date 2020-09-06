package sk.spring.config;

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

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sk.services.async.AsyncImpl;
import sk.services.bytes.BytesImpl;
import sk.services.except.IExcept;
import sk.services.free.Freemarker;
import sk.services.http.HttpImpl;
import sk.services.ids.IdsImpl;
import sk.services.json.IJson;
import sk.services.json.JGsonImpl;
import sk.services.log.ILog;
import sk.services.rand.RandImpl;
import sk.services.rescache.ResCacheImpl;
import sk.services.retry.RepeatImpl;
import sk.services.shutdown.AppStopService;
import sk.services.time.TimeUtcImpl;
import sk.spring.services.BootServiceImpl;
import sk.spring.services.CoreServices;
import sk.spring.services.ServiceProvider4SpringImpl;

@Configuration
@Log4j2
public class SpringCoreConfig {
    @Bean
    public TimeUtcImpl ITime() {return new TimeUtcImpl();}

    @Bean
    public RandImpl RandImpl() {return new RandImpl();}

    @Bean
    public AsyncImpl AsyncImpl() {return new AsyncImpl();}

    @Bean
    public BytesImpl BytesImpl() {return new BytesImpl();}

    @Bean
    public HttpImpl HttpImpl() {return new HttpImpl();}

    @Bean
    public IdsImpl IdsImpl() {return new IdsImpl();}

    @Bean
    public ILog ILog(IJson json) {
        return (severity, category, type, info, logType) -> {
            switch (severity) {
                case ERROR:
                    log.error(category + "__" + type + "_" + json.to(info));
                    break;
                case INFO:
                    log.info(category + "__" + type + "_" + json.to(info));
                    break;
                case DEBUG:
                    log.debug(category + "__" + type + "_" + json.to(info));
                    break;
                case TRACE:
                    log.trace(category + "__" + type + "_" + json.to(info));
                    break;
            }
        };
    }

    @Bean
    public ResCacheImpl ResCache() {return new ResCacheImpl();}

    @Bean
    public RepeatImpl RetryImpl() {return new RepeatImpl();}

    @Bean
    public AppStopService AppStopService() {return new AppStopService();}

    @Bean
    public BootServiceImpl BootServiceImpl() {return new BootServiceImpl();}

    @Bean
    public JGsonImpl JsonJacksonImpl() {return new JGsonImpl();}

    @Bean
    public ServiceProvider4SpringImpl IServiceProvider() {
        return new ServiceProvider4SpringImpl();
    }

    @Bean
    public IExcept IExcept() {
        return new IExcept() {};
    }

    @Bean
    public Freemarker Freemarker() {
        return new Freemarker();
    }

    @Bean
    public CoreServices CoreServices() {
        return new CoreServices();
    }
}
