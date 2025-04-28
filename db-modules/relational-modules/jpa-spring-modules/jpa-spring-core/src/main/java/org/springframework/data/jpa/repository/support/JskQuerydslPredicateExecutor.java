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

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Main intention is to fix the findAll not to select count(*) every time
 *
 * @param <T>
 * @see org.springframework.data.querydsl.QuerydslPredicateExecutor
 */
public interface JskQuerydslPredicateExecutor<T> {
    /**
     * Returns a single entity matching the given {@link Predicate} or {@link Optional#empty()} if none was found.
     *
     * @param predicate must not be {@literal null}.
     * @return a single entity matching the given {@link Predicate} or {@link Optional#empty()} if none was found.
     * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if the predicate yields more than one
     *                                                                        result.
     */
    Optional<T> findOne(Predicate predicate);

    /**
     * Returns all entities matching the given {@link Predicate}. In case no match could be found an empty
     * {@link Iterable} is returned.
     *
     * @param predicate must not be {@literal null}.
     * @return all entities matching the given {@link Predicate}.
     */
    List<T> findAll(Predicate predicate);


    /**
     * Returns all entities matching the given {@link Predicate} applying the given {@link OrderSpecifier}s. In case no
     * match could be found an empty {@link Iterable} is returned.
     *
     * @param predicate must not be {@literal null}.
     * @param orders    the {@link OrderSpecifier}s to sort the results by, must not be {@literal null}.
     * @return all entities matching the given {@link Predicate} applying the given {@link OrderSpecifier}s.
     */
    List<T> findAll(Predicate predicate, OrderSpecifier<?>... orders);

    /**
     * Returns all entities ordered by the given {@link OrderSpecifier}s.
     *
     * @param orders the {@link OrderSpecifier}s to sort the results by, must not be {@literal null}.
     * @return all entities ordered by the given {@link OrderSpecifier}s.
     */
    List<T> findAll(OrderSpecifier<?>... orders);

    /**
     * Returns a {@link Page} of entities matching the given {@link Predicate}. In case no match could be found, an empty
     * {@link Page} is returned.
     *
     * @param predicate must not be {@literal null}.
     * @param pageable  may be {@link Pageable#unpaged()}, must not be {@literal null}.
     * @return a {@link Page} of entities matching the given {@link Predicate}.
     */
    List<T> findAll(Predicate predicate, Pageable pageable);

    /**
     * Returns the number of instances matching the given {@link Predicate}.
     *
     * @param predicate the {@link Predicate} to count instances for, must not be {@literal null}.
     * @return the number of instances matching the {@link Predicate}.
     */
    long count(Predicate predicate);

}
