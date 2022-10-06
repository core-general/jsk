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

import jsk.gcl.srv.scaling.schedulers.GclJobGroupArchiveScheduler;
import jsk.gcl.srv.scaling.schedulers.GclJobLockScheduler;
import jsk.gcl.srv.scaling.schedulers.GclNodeArchiveScheduler;
import jsk.gcl.srv.scaling.schedulers.GclScalingDataGatherScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GclSchedulingConfig {
    @Bean
    GclJobGroupArchiveScheduler GclJobGroupArchiveScheduler() {
        return new GclJobGroupArchiveScheduler();
    }

    @Bean
    GclScalingDataGatherScheduler GclScalingDataGatherScheduler() {
        return new GclScalingDataGatherScheduler();
    }

    @Bean
    GclJobLockScheduler GclJobLockScheduler() {
        return new GclJobLockScheduler();
    }

    @Bean
    GclNodeArchiveScheduler GclNodeArchiveScheduler() {
        return new GclNodeArchiveScheduler();
    }
}
