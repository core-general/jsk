package sk.db.kv;

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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.JskJpaRepositoryFactoryBean;
import org.springframework.data.repository.query.QueryLookupStrategy;
import sk.db.relational.spring.proprties.RdbJpaPackages;
import sk.utils.statics.Cc;

@Configuration
@EnableJpaRepositories(
        repositoryFactoryBeanClass = JskJpaRepositoryFactoryBean.class,
        value = {
                "sk.db.kv",
        },
        queryLookupStrategy = QueryLookupStrategy.Key.CREATE
)
public class RdbKvConfig {
    @Bean
    RdbKVStoreImpl RdbKVStoreImpl() {
        return new RdbKVStoreImpl();
    }

    @Bean
    RdbJpaPackages RdbJpaPackagesRdbKv() {return () -> Cc.l("sk.db.kv");}
}
