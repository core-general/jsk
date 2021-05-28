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
import sk.services.ICoreServices;
import sk.services.async.AsyncImpl;
import sk.services.async.IAsync;
import sk.services.async.ISizedSemaphore;
import sk.services.async.ISizedSemaphoreImpl;
import sk.services.bean.IServiceProvider;
import sk.services.bytes.BytesImpl;
import sk.services.bytes.IBytes;
import sk.services.except.IExcept;
import sk.services.free.Freemarker;
import sk.services.free.IFree;
import sk.services.http.HttpImpl;
import sk.services.http.IHttp;
import sk.services.ids.IIds;
import sk.services.ids.IdsImpl;
import sk.services.json.IJson;
import sk.services.json.JGsonImpl;
import sk.services.log.ILog;
import sk.services.log.ILogConsoleImpl;
import sk.services.mapping.IMapper;
import sk.services.mapping.ModelMapperImpl;
import sk.services.rand.IRand;
import sk.services.rand.RandImpl;
import sk.services.rescache.IResCache;
import sk.services.rescache.ResCacheImpl;
import sk.services.retry.IRepeat;
import sk.services.retry.RepeatImpl;
import sk.services.shutdown.AppStopService;
import sk.services.time.ITime;
import sk.services.time.TimeUtcImpl;
import sk.spring.services.BootServiceImpl;
import sk.spring.services.CoreServices;
import sk.spring.services.ServiceProvider4SpringImpl;

@Configuration
@Log4j2
public class SpringCoreConfig implements ICoreServices {
    @Bean
    public ITime times() {return new TimeUtcImpl();}

    @Bean
    public IRand rand() {return new RandImpl();}

    @Bean
    public IAsync async() {return new AsyncImpl();}

    @Bean
    public IBytes bytes() {return new BytesImpl();}

    @Bean
    public IHttp http() {return new HttpImpl();}

    @Bean
    public IIds ids() {return new IdsImpl();}

    @Bean
    public ILog iLog() {
        return new ILogConsoleImpl();
    }

    @Bean
    public IMapper iMapper() {
        return new ModelMapperImpl();
    }

    @Bean
    public IResCache resCache() {return new ResCacheImpl();}

    @Bean
    public IRepeat repeat() {return new RepeatImpl();}

    @Bean
    public AppStopService AppStopService() {return new AppStopService();}

    @Bean
    public BootServiceImpl BootServiceImpl() {return new BootServiceImpl();}

    @Bean
    public IJson json() {return new JGsonImpl();}

    @Bean
    public IServiceProvider IServiceProvider() {
        return new ServiceProvider4SpringImpl();
    }

    @Bean
    public IExcept except() {
        return new IExcept() {};
    }

    @Bean
    public IFree free() {
        return new Freemarker();
    }

    @Bean
    public ISizedSemaphore sizedSemaphore() {
        return new ISizedSemaphoreImpl(Math.min(Runtime.getRuntime().maxMemory() / 5, 200_000_000), 10, 50L);
    }

    @Bean
    public CoreServices CoreServices() {
        return new CoreServices();
    }

}
