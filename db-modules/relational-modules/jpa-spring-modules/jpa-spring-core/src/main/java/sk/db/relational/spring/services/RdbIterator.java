package sk.db.relational.spring.services;

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

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.SimpleExpression;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.querydsl.QPageRequest;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import sk.db.relational.utils.ReadWriteRepo;
import sk.services.async.IAsync;
import sk.services.log.ILog;
import sk.services.log.ILogCategory;
import sk.services.profile.IAppProfile;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;

import javax.inject.Inject;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static sk.utils.statics.Cc.m;

@SuppressWarnings({"ForLoopReplaceableByForEach", "UnnecessaryLabelOnBreakStatement", "unused"})
@AllArgsConstructor
@NoArgsConstructor
public class RdbIterator {
    public static final int SPLIT_SIZE = 10000;
    @Inject ILog log;
    @Inject IAsync async;
    @Inject Optional<IAppProfile> profile = Optional.empty();

    @Inject NamedParameterJdbcOperations jdbc;

    /**
     * When full table scan is needed, it processes items page per page (1000 items in page)
     */
    public <T, ID extends Serializable> long iterate(
            Consumer<T> toApply,
            ILogCategory logCategory,
            String logSubCategorySingleFail,
            ReadWriteRepo<T, ID> repo,
            Predicate query,
            OrderSpecifier<?>... ordering) {
        return iterate(toApply, logCategory, logSubCategorySingleFail, repo, query, 1, ordering);
    }

    public <T, ID extends Serializable> long iterate(
            Consumer<T> toApply,
            ILogCategory logCategory,
            String logSubCategorySingleFail,
            ReadWriteRepo<T, ID> repo,
            Predicate query,
            int threadCount,
            OrderSpecifier<?>... ordering) {
        int pageSize = profile.map($ -> $.getProfile().isForDefaultTesting()).orElse(false) ? 5 : 1000;
        int pageNumber = 0;
        long itemCount = 0;
        finish:
        while (pageNumber < Integer.MAX_VALUE) {
            Page<T> all = repo.findAll(query,
                    ordering.length > 0
                    ? QPageRequest.of(pageNumber, pageSize, ordering)
                    : QPageRequest.of(pageNumber, pageSize));

            List<T> content = all.getContent();


            final F1<Boolean, Long> summator =
                    (parallel) -> (parallel ? content.parallelStream() : content.stream()).mapToLong(row -> {
                        try {
                            toApply.accept(row);
                            return 1;
                        } catch (Exception e) {
                            log.logError(logCategory, logSubCategorySingleFail, m("row", row + "", "e", Ex.getInfo(e)));
                            return 0;
                        }
                    }).sum();
            itemCount += threadCount == 1 ?
                         summator.apply(false) :
                         async.coldTaskFJPGet(threadCount, () -> summator.apply(true));
            if (all.getContent().size() < pageSize) {
                break finish;
            }
            pageNumber++;
        }
        return itemCount;
    }

    /**
     * It's impossible to pass more than ~30k items in " in (...)" query, so we have to split request to a number of queries and
     * merge results. Replaces findAllByIds
     */
    public <ITEM, ID extends Serializable> List<ITEM> getManyItemsByIds(
            Collection<ID> ids,
            QuerydslPredicateExecutor<ITEM> repo,
            SimpleExpression<ID> idExpression,
            O<Predicate> additionalWhere,
            boolean parallel) {
        if (ids.size() < SPLIT_SIZE) {
            return Cc.list(repo.findAll(formPredicate(ids, idExpression, additionalWhere)));
        } else {
            int split = (ids.size() / SPLIT_SIZE) + 1;
            final Collection<List<ID>> values =
                    ((parallel) ? ids.parallelStream() : ids.stream())
                            .distinct()
                            .collect(Collectors.groupingBy($ -> Math.abs($.hashCode() % split)))
                            .values();
            return ((parallel) ? values.parallelStream() : values.stream())
                    .flatMap($ -> Cc.stream(repo.findAll(formPredicate($, idExpression, additionalWhere))))
                    .collect(Cc.toL());
        }
    }

    /**
     * Count(*) counts items, so is very slow, this method allows to get data directly from metatables, so it's fast
     * itemCls should be annotated with Table annotation
     */
    public O<Long> getFastItemCountInPostgresDb(Class<?> itemCls) {
        final Table table = itemCls.getAnnotation(Table.class);
        if (table == null) {
            return O.empty();
        } else {
            try {
                final String schema = table.schema();
                final String tName = table.name();
                final Long n_live_tup = jdbc.query(
                        format("select n_live_tup from pg_catalog.pg_stat_all_tables where schemaname='%s' and relname='%s'",
                                schema, tName),
                        rs -> {
                            rs.next();
                            return rs.getLong("n_live_tup");
                        });
                return O.of(n_live_tup);
            } catch (Exception e) {
                return O.empty();
            }
        }
    }

    private <ID extends Serializable> BooleanExpression formPredicate(Collection<ID> ids, SimpleExpression<ID> idExpression,
            O<Predicate> additionalWhere) {
        BooleanExpression in = idExpression.in(ids);
        if (additionalWhere.isPresent()) {
            in = in.and(additionalWhere.get());
        }
        return in;
    }
}
