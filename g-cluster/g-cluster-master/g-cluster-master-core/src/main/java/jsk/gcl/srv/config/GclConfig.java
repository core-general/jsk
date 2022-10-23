package jsk.gcl.srv.config;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import jsk.gcl.srv.logic.jobs.services.GclJobManager;
import jsk.gcl.srv.logic.scaling.GclOOMManager;
import jsk.gcl.srv.logic.scaling.workers.GclScalingLocalWorkerManager;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import sk.spring.utils.DefaultThrowableHandler;

import javax.inject.Inject;

@Configuration
@Log4j2
@Import(value = {
        GclSchedulingConfig.class,
        GclDbConfig.class
})
public class GclConfig {
    @Bean
    GclScalingLocalWorkerManager GclScalingLocalWorkerManager() {
        return new GclScalingLocalWorkerManager();
    }

    @Bean
    GclJobManager GclJobManager() {
        return new GclJobManager();
    }

    @Bean
    DefaultThrowableHandler<OutOfMemoryError> DefaultThrowableHandlerOOM() {
        return new DefaultThrowableHandler<>() {
            @Inject GclOOMManager oomManager;

            @Override
            public Class<OutOfMemoryError> getThrowableClass() {
                return OutOfMemoryError.class;
            }

            @Override
            public void process(OutOfMemoryError throwable, Thread t) {
                log.error("!OUT OF MEMORY!", throwable);
                oomManager.onOOM(throwable, t);
            }
        };
    }

}
