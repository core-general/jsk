package org.springframework.data.jpa.repository.support;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2025 Core General
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

import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * The intention is to create JskJpaRepositoryFactory (with better Querydsl support)
 * rather than JpaRepositoryFactory
 *
 * @param <T>
 * @param <S>
 * @param <ID>
 */
public class JskJpaRepositoryFactoryBean<T extends Repository<S, ID>, S, ID> extends JpaRepositoryFactoryBean<T, S, ID> {
    public JskJpaRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    /**
     * Returns a {@link RepositoryFactorySupport}.
     */
    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        JskJpaRepositoryFactory jpaRepositoryFactory = new JskJpaRepositoryFactory(entityManager);
        jpaRepositoryFactory.setEntityPathResolver(new SimpleEntityPathResolver(""));
        jpaRepositoryFactory.setEscapeCharacter(EscapeCharacter.DEFAULT);
        return jpaRepositoryFactory;
    }
}
