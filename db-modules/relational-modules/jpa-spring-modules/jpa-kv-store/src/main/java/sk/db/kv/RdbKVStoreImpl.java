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

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.extern.log4j.Log4j2;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.querydsl.QPageRequest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import sk.services.kv.*;
import sk.services.kv.keys.KvKey;
import sk.services.kv.keys.KvKeyWithDefault;
import sk.services.kv.keys.KvLockOrRenewKey;
import sk.services.time.ITime;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;

import javax.inject.Inject;
import javax.persistence.OptimisticLockException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static sk.db.kv.QJpaKVItem.jpaKVItem;
import static sk.db.kv.QJpaKVItemWithRaw.jpaKVItemWithRaw;

@Log4j2
public class RdbKVStoreImpl extends IKvStoreJsonBased implements IKvLimitedStore {
    @Inject NamedParameterJdbcOperations jdbcQuery;
    @Inject LocalContainerEntityManagerFactoryBean factory;
    @Inject JpaKVRepo kv;
    @Inject JpaKVWithRawRepo kvRaw;
    @Inject ITime times;

    @Override
    public O<KvVersionedItem<String>> getRawVersioned(KvKeyWithDefault key) {
        KVItemId _key = new KVItemId(key);
        return O.of(kv.findById(_key))
                .flatMap(item -> checkTtl(_key, item, t -> null))
                .map($ -> new KvVersionedItem<>(key, $.getValue(), O.empty(), $.getCreatedAt(), $.getVersion()));
    }

    @Override
    public O<KvVersionedItemAll<String>> getRawVersionedAll(KvKeyWithDefault key) {
        KVItemId _key = new KVItemId(key);
        return O.of(kvRaw.findById(_key))
                .flatMap(item -> checkTtl(_key, item, t -> null))
                .map($ -> new KvVersionedItemAll<>(key,
                        new KvAllValues<>($.getValue(), O.ofNull($.getRawValue()), O.empty()),
                        O.empty(), $.getCreatedAt(), $.getVersion()));
    }

    @Override
    public List<KvListItemAll<String>> getRawVersionedListBetweenCategories(KvKey baseKey, O<String> fromLastCategory,
            O<String> toLastCategory, int maxCount, boolean ascending) {
        BooleanExpression predicate = jpaKVItemWithRaw.id.key1.eq(new KVItemId(baseKey).getKey1());
        if (fromLastCategory.isPresent()) {
            predicate = predicate.and(jpaKVItemWithRaw.id.key2.goe(fromLastCategory.get()));
        }
        if (toLastCategory.isPresent()) {
            predicate = predicate.and(jpaKVItemWithRaw.id.key2.loe(toLastCategory.get()));
        }
        final Page<JpaKVItemWithRaw> values = kvRaw.findAll(predicate, QPageRequest.of(0, maxCount,
                ascending ? jpaKVItemWithRaw.id.key2.asc() : jpaKVItemWithRaw.id.key2.desc()));

        return values.get()
                .flatMap($ -> checkTtl($.getId(), $, t -> null).stream())
                .map($ -> new KvListItemAll<>($.getId().toKvKey(), $.getValue(), O.ofNull($.getRawValue()), $.getCreatedAt()))
                .limit(maxCount)
                .collect(Cc.toL());
    }

    @Override
    public OneOf<Boolean, Exception> trySaveNewString(KvKey key, String newValueProvider) {
        try {
            kv.save(new JpaKVItem(new KVItemId(key), newValueProvider, 0l, null, null, null));
            return OneOf.left(true);
        } catch (OptimisticEntityLockException | ObjectOptimisticLockingFailureException
                | OptimisticLockException | StaleObjectStateException e) {
            return OneOf.left(false);
        } catch (Exception e) {
            return OneOf.right(e);
        }
    }

    @Override
    public OneOf<Boolean, Exception> trySaveNewStringAndRaw(KvKey key, KvAllValues<String> newValueProvider) {
        try {
            kvRaw.save(new JpaKVItemWithRaw(new KVItemId(key), newValueProvider.getValue(),
                    0l, newValueProvider.getRawValue().orElse(null), null, null, null));
            return OneOf.left(true);
        } catch (OptimisticEntityLockException | ObjectOptimisticLockingFailureException
                | OptimisticLockException | StaleObjectStateException e) {
            return OneOf.left(false);
        } catch (Exception e) {
            return OneOf.right(e);
        }
    }

    @Override
    public boolean tryLockOrRenew(KvLockOrRenewKey k, String whoLocks, O<Long> lockIsOldAfterOrElseRenew) {
        KVItemId id = new KVItemId(k);
        KvVersionedItem<String> kvItem = getOrCreateNew(k, id);
        long now = times.now();
        Map<String, Object> paramMap = Cc.m(
                "v", whoLocks,
                "k1", k.getKey1(),
                "k2", k.getKey2().orElse(""),
                "ver", kvItem.getVersion(),
                "now", now
        );
        if (lockIsOldAfterOrElseRenew.isPresent()) {
            paramMap.put("date_check", now - lockIsOldAfterOrElseRenew.get());
        }
        String query = String.format(
                "update %s._general_purpose_kv set value=:v, lock_date=:now  where key1=:k1 and key2=:k2 %s and version=:ver",
                factory.getJpaPropertyMap().get("hibernate.default_schema"),
                lockIsOldAfterOrElseRenew.isPresent() ? "and lock_date<:date_check " : " and value =:v ");
        int update = jdbcQuery.update(query, paramMap);
        return update == 1;
    }

    @Override
    public void clearValue(KvKey key) {
        clearValuePrivate(new KVItemId(key));
    }

    @Override
    public OneOf<O<KvAllValues<String>>, Exception> updateStringAndRaw(KvKeyWithDefault key,
            F1<KvAllValues<String>, O<KvAllValues<String>>> updater) {
        int k = 10000;
        KVItemId _key = new KVItemId(key);

        while (k-- >= 0) {
            try {
                KvVersionedItemAll<String> raw = getOrCreateNewAllValues(key, _key);
                O<KvAllValues<String>> apply = O.empty();
                apply = updater.apply(raw.getVals());
                if (apply.isEmpty()) {
                    return OneOf.left(O.empty());
                }
                kvRaw.save(new JpaKVItemWithRaw(_key, apply.get().getValue(),
                        0l, apply.get().getRawValue().orElse(null), raw.getCreated(),
                        null, (Long) raw.getVersion()));
                return OneOf.left(apply);
            } catch (OptimisticEntityLockException | ObjectOptimisticLockingFailureException
                    | OptimisticLockException | StaleObjectStateException e) {
                continue;
            } catch (Exception e) {
                return OneOf.right(e);
            }
        }
        return OneOf.right(new OptimisticLockException("Can't save kvKey:" + key));
    }

    @Override
    public OneOf<O<String>, Exception> updateString(KvKeyWithDefault key, F1<String, O<String>> updater) {
        int k = 10000;

        while (k-- >= 0) {
            try {
                KVItemId _key = new KVItemId(key);
                KvVersionedItem<String> raw = getOrCreateNew(key, _key);
                O<String> apply = O.empty();
                apply = updater.apply(raw.getValue());

                if (apply.isEmpty()) {
                    return OneOf.left(O.empty());
                }
                kv.save(new JpaKVItem(_key, apply.get(), 0l, raw.getCreated(), null,
                        (Long) raw.getVersion()));
                return OneOf.left(apply);
            } catch (OptimisticEntityLockException | ObjectOptimisticLockingFailureException
                    | OptimisticLockException | StaleObjectStateException e) {
                continue;
            } catch (Exception e) {
                return OneOf.right(e);
            }
        }
        return OneOf.right(new OptimisticLockException("Can't save kvKey:" + key));
    }

    @Override
    public void clearAll() {
        kv.deleteAll();
    }

    @Override
    public void clearAll(O<List<KvKey>> except) {
        if (except.orElse(Cc.lEmpty()).size() == 0) {
            clearAll();
            return;
        }
        final List<BooleanExpression> expressions = except.orElse(Cc.lEmpty()).stream()
                .map($ -> {
                    if ($.categories().size() == 1) {
                        return jpaKVItem.id.key1.eq($.categories().get(0));
                    } else {
                        return jpaKVItem.id.key1.eq($.categories().get(0)).and(jpaKVItem.id.key2.eq($.categories().get(1)));
                    }
                })
                .collect(Collectors.toList());
        BooleanExpression mustPersist = expressions.get(0);
        for (int i = 1; i < expressions.size(); i++) {
            mustPersist = mustPersist.or(expressions.get(i));
        }

        final Iterable<JpaKVItem> mustBeDeleted = kv.findAll(mustPersist.not());
        kv.deleteAll(mustBeDeleted);
    }

    @NotNull
    private KvVersionedItem<String> getOrCreateNew(KvKeyWithDefault key, KVItemId id) {
        return getRawVersioned(key).orElseGet(() -> {
            trySaveNewString(key, key.getDefaultValue());
            JpaKVItem one = kv.findById(id).get();
            return new KvVersionedItem<>(key, one.getValue(), O.empty(), null, one.getVersion());
        });
    }

    private KvVersionedItemAll<String> getOrCreateNewAllValues(KvKeyWithDefault key, KVItemId id) {
        return getRawVersionedAll(key).orElseGet(() -> {
            trySaveNewStringAndRaw(key, new KvAllValues<>(key.getDefaultValue(), O.empty(), O.empty()));
            JpaKVItemWithRaw one = kvRaw.findById(id).get();
            return new KvVersionedItemAll<>(key,
                    new KvAllValues<>(one.getValue(), O.ofNull(one.getRawValue()), O.empty()),
                    O.empty(),
                    null, one.getVersion());
        });
    }

    private void clearValuePrivate(KVItemId id) {
        kvRaw.deleteById(id);
    }

    private <T> O<T> checkTtl(KVItemId id, T item, F1<T, ZonedDateTime> getTtl) {
        return O.ofNull(getTtl.apply(item)).flatMap($ -> {
            if (times.nowZ().isAfter($)) {
                clearValuePrivate(id);
                return O.empty();
            } else {
                return O.of(item);
            }
        }).or(() -> O.of(item));
    }
}

