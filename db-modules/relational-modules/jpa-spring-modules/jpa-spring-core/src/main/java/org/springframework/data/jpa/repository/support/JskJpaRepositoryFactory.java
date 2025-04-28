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
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;

import static org.springframework.data.querydsl.QuerydslUtils.QUERY_DSL_PRESENT;

public class JskJpaRepositoryFactory extends JpaRepositoryFactory {

    public JskJpaRepositoryFactory(EntityManager entityManager) {
        super(entityManager);
    }

    protected RepositoryComposition.RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata,
            EntityManager entityManager,
            EntityPathResolver resolver, CrudMethodMetadata crudMethodMetadata) {

        boolean isQueryDslRepository = QUERY_DSL_PRESENT
                                       && JskQuerydslPredicateExecutor.class.isAssignableFrom(metadata.getRepositoryInterface());

        if (isQueryDslRepository) {

            if (metadata.isReactiveRepository()) {
                throw new InvalidDataAccessApiUsageException(
                        "Cannot combine Querydsl and reactive repository support in a single interface");
            }

            return RepositoryComposition.RepositoryFragments.just(
                    new JskQuerydslJpaRepositoryImpl<>(getEntityInformation(metadata.getDomainType()),
                            entityManager, resolver, crudMethodMetadata));
        }

        return RepositoryComposition.RepositoryFragments.empty();
    }
}
