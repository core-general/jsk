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

import jsk.gcl.srv.jpa.GclStorageFacadeImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import sk.db.relational.spring.proprties.RdbJpaPackages;
import sk.utils.statics.Cc;

@EnableJpaRepositories(
        value = {
                "jsk.gcl.srv.jpa",
        },
        queryLookupStrategy = QueryLookupStrategy.Key.CREATE
)
@Configuration
@EnableTransactionManagement
public class GclDbConfig {
    @Bean
    RdbJpaPackages RdbJpaPackagesYoa() {
        return () -> Cc.l("jsk.gcl.srv.jpa");
    }

    @Bean
    GclStorageFacadeImpl GclStorageFacadeImpl() {return new GclStorageFacadeImpl();}
}
