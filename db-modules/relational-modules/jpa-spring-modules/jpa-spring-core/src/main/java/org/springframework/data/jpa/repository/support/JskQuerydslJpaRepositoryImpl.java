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

import com.querydsl.core.NonUniqueResultException;
import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.AbstractJPAQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.query.KeysetScrollDelegate;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QSort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

/**
 * Main intention is to fix the findAll not to select count(*) every time
 *
 * @param <T>
 * @see org.springframework.data.querydsl.QuerydslPredicateExecutor
 */
public class JskQuerydslJpaRepositoryImpl<T> implements JskQuerydslPredicateExecutor<T> {
    private final JpaEntityInformation<T, ?> entityInformation;
    private final EntityPath<T> path;
    private final Querydsl querydsl;
    private final QuerydslQueryStrategy scrollQueryAdapter;
    private final EntityManager entityManager;
    private final CrudMethodMetadata metadata;

    /**
     * Creates a new {@link QuerydslJpaPredicateExecutor} from the given domain class and {@link EntityManager} and uses
     * the given {@link EntityPathResolver} to translate the domain class into an {@link EntityPath}.
     *
     * @param entityInformation must not be {@literal null}.
     * @param entityManager     must not be {@literal null}.
     * @param resolver          must not be {@literal null}.
     * @param metadata          maybe {@literal null}.
     */
    public JskQuerydslJpaRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager,
            EntityPathResolver resolver, @Nullable CrudMethodMetadata metadata) {

        this.entityInformation = entityInformation;
        this.metadata = metadata;
        this.path = resolver.createPath(entityInformation.getJavaType());
        this.querydsl = new Querydsl(entityManager, new PathBuilder<T>(path.getType(), path.getMetadata()));
        this.entityManager = entityManager;
        this.scrollQueryAdapter = new QuerydslQueryStrategy();
    }

    @Override
    public Optional<T> findOne(Predicate predicate) {

        Assert.notNull(predicate, "Predicate must not be null");

        try {
            return Optional.ofNullable(createQuery(predicate).select(path).limit(2).fetchOne());
        } catch (NonUniqueResultException ex) {
            throw new IncorrectResultSizeDataAccessException(ex.getMessage(), 1, ex);
        }
    }

    @Override
    public List<T> findAll(Predicate predicate) {

        Assert.notNull(predicate, "Predicate must not be null");

        return createQuery(predicate).select(path).fetch();
    }

    @Override
    public List<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {

        Assert.notNull(predicate, "Predicate must not be null");
        Assert.notNull(orders, "Order specifiers must not be null");

        return executeSorted(createQuery(predicate).select(path), orders);
    }


    @Override
    public List<T> findAll(OrderSpecifier<?>... orders) {

        Assert.notNull(orders, "Order specifiers must not be null");

        return executeSorted(createQuery(new Predicate[0]).select(path), orders);
    }

    @Override
    public List<T> findAll(Predicate predicate, Pageable pageable) {

        Assert.notNull(predicate, "Predicate must not be null");
        Assert.notNull(pageable, "Pageable must not be null");

        JPQLQuery<T> query = querydsl.applyPagination(pageable, createQuery(predicate).select(path));

        return query.fetch();
    }

    @Override
    public long count(Predicate predicate) {
        return createQuery(predicate).fetchCount();
    }


    /**
     * Creates a new {@link JPQLQuery} for the given {@link Predicate}.
     *
     * @param predicate
     * @return the Querydsl {@link JPQLQuery}.
     */
    protected AbstractJPAQuery<?, ?> createQuery(Predicate... predicate) {

        Assert.notNull(predicate, "Predicate must not be null");

        AbstractJPAQuery<?, ?> query = doCreateQuery(getQueryHints().withFetchGraphs(entityManager), predicate);
        CrudMethodMetadata metadata = getRepositoryMethodMetadata();

        if (metadata == null) {
            return query;
        }

        LockModeType type = metadata.getLockModeType();
        return type == null ? query : query.setLockMode(type);
    }

    /**
     * Creates a new {@link JPQLQuery} count query for the given {@link Predicate}.
     *
     * @param predicate, can be {@literal null}.
     * @return the Querydsl count {@link JPQLQuery}.
     */
    protected JPQLQuery<?> createCountQuery(@Nullable Predicate... predicate) {
        return doCreateQuery(getQueryHintsForCount(), predicate);
    }

    @Nullable
    private CrudMethodMetadata getRepositoryMethodMetadata() {
        return metadata;
    }

    /**
     * Returns {@link QueryHints} with the query hints based on the current {@link CrudMethodMetadata} and potential
     * {@link EntityGraph} information.
     *
     * @return
     */
    private QueryHints getQueryHints() {
        return metadata == null ? QueryHints.NoHints.INSTANCE : DefaultQueryHints.of(entityInformation, metadata);
    }

    /**
     * Returns {@link QueryHints} with the query hints based on the current {@link CrudMethodMetadata} and potential
     * {@link EntityGraph} information and filtered for those hints that are to be applied to count queries.
     *
     * @return
     */
    private QueryHints getQueryHintsForCount() {
        return metadata == null ? QueryHints.NoHints.INSTANCE
                                : DefaultQueryHints.of(entityInformation, metadata).forCounts();
    }

    private AbstractJPAQuery<?, ?> doCreateQuery(QueryHints hints, @Nullable Predicate... predicate) {

        AbstractJPAQuery<?, ?> query = querydsl.createQuery(path);

        if (predicate != null) {
            query = query.where(predicate);
        }

        hints.forEach(query::setHint);

        return query;
    }

    /**
     * Executes the given {@link JPQLQuery} after applying the given {@link OrderSpecifier}s.
     *
     * @param query  must not be {@literal null}.
     * @param orders must not be {@literal null}.
     * @return
     */
    private List<T> executeSorted(JPQLQuery<T> query, OrderSpecifier<?>... orders) {
        return executeSorted(query, new QSort(orders));
    }

    /**
     * Executes the given {@link JPQLQuery} after applying the given {@link Sort}.
     *
     * @param query must not be {@literal null}.
     * @param sort  must not be {@literal null}.
     * @return
     */
    private List<T> executeSorted(JPQLQuery<T> query, Sort sort) {
        return querydsl.applySorting(sort, query).fetch();
    }

    class QuerydslQueryStrategy implements KeysetScrollDelegate.QueryStrategy<Expression<?>, BooleanExpression> {

        @Override
        public Expression<?> createExpression(String property) {
            return querydsl.createExpression(property);
        }

        @Override
        public BooleanExpression compare(Sort.Order order, Expression<?> propertyExpression, Object value) {
            return Expressions.booleanOperation(order.isAscending() ? Ops.GT : Ops.LT, propertyExpression,
                    ConstantImpl.create(value));
        }

        @Override
        public BooleanExpression compare(Expression<?> propertyExpression, @Nullable Object value) {
            return Expressions.booleanOperation(Ops.EQ, propertyExpression,
                    value == null ? NullExpression.DEFAULT : ConstantImpl.create(value));
        }

        @Override
        public BooleanExpression and(List<BooleanExpression> intermediate) {
            return Expressions.allOf(intermediate.toArray(new BooleanExpression[0]));
        }

        @Override
        public BooleanExpression or(List<BooleanExpression> intermediate) {
            return Expressions.anyOf(intermediate.toArray(new BooleanExpression[0]));
        }
    }
}
