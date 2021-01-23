package sk.aws.dynamo;

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

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import sk.exceptions.NotImplementedException;
import sk.services.async.IAsync;
import sk.services.ids.IIds;
import sk.services.kv.*;
import sk.services.kv.keys.KvKey;
import sk.services.kv.keys.KvKeyRaw;
import sk.services.kv.keys.KvKeyWithDefault;
import sk.services.kv.keys.KvLockOrRenewKey;
import sk.services.profile.IAppProfile;
import sk.services.retry.IRepeat;
import sk.services.time.ITime;
import sk.utils.functional.*;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.joining;
import static sk.utils.statics.St.endWith;

@Log4j2
public class DynKVStoreImpl extends IKvStoreJsonBased implements IKvUnlimitedStore {
    @Inject DynamoDbEnhancedClient dynaHighLvl;
    @Inject DynamoDbClient dynaLowLvl;
    @Inject DynProperties conf;

    @Inject IAsync async;
    @Inject IRepeat repeat;
    @Inject ITime times;
    @Inject IIds ids;
    @Inject IAppProfile<?> appProfile;

    @Override
    public O<KvVersionedItem<String>> getRawVersioned(KvKeyWithDefault key) {
        return withTableEnsure(key, table -> O.ofNull(privateItemGet(table, key)).map($ ->
                new KvVersionedItem<>(key, $.getValue(), O.ofNull($.getTtl()).map(x -> times.toZDT(x)),
                        O.ofNull($.getCreatedAt()).orElse(null),
                        $.getVersion())));
    }

    @Override
    public O<KvVersionedItemAll<String>> getRawVersionedAll(KvKeyWithDefault key) {
        return withTableEnsure(key,
                table -> O.ofNull(privateItemGet(table, key))
                        .map($ -> {
                            final O<Long> ttl = O.ofNull($.getTtl());
                            final O<ZonedDateTime> ttlZdt = ttl.map(x -> times.toZDT(x));
                            return new KvVersionedItemAll<>(key,
                                    new KvAllValues<>($.getValue(), O.ofNull($.getRawValue()), ttlZdt),
                                    ttlZdt, O.ofNull($.getCreatedAt()).orElse(null), $.getVersion());
                        }));
    }

    @Override
    public List<KvListItemAll<String>> getRawVersionedListBetweenCategories(
            KvKey baseKey, O<String> fromLastCategory,
            O<String> toLastCategory, int maxCount, boolean ascending) {
        return withTableEnsure(baseKey, table -> {
            final O<Key> fromKey = fromLastCategory
                    .map($ -> toAwsKey(baseKey, (k1, k2) -> Key.builder().partitionValue(k1).sortValue($).build()));
            final O<Key> toKey = toLastCategory
                    .map($ -> toAwsKey(baseKey, (k1, k2) -> Key.builder().partitionValue(k1).sortValue($).build()));
            QueryConditional query;
            if (fromKey.isPresent() && toKey.isPresent()) {
                query = QueryConditional.sortBetween(fromKey.get(), toKey.get());
            } else if (fromKey.isPresent() && toKey.isEmpty()) {
                query = QueryConditional.sortGreaterThanOrEqualTo(fromKey.get());
            } else if (fromKey.isEmpty() && toKey.isPresent()) {
                query = QueryConditional.sortLessThanOrEqualTo(toKey.get());
            } else {
                query = QueryConditional.keyEqualTo(
                        toAwsKey(baseKey, (k1, k2) -> Key.builder().partitionValue(k1).build())
                );
            }

            final PageIterable<DynKVItem> iterable = table.query(QueryEnhancedRequest.builder()
                    .queryConditional(query)
                    .consistentRead(true)
                    .limit(maxCount)
                    .scanIndexForward(ascending)
                    .build());

            return iterable.items().stream()
                    .limit(maxCount)
                    .map($ -> new KvListItemAll<>(toKvKeyWithOtherKey2(baseKey, $.getKey1(), $.getKey2()), $.getValue(),
                            O.ofNull($.getRawValue()), O.ofNull($.getCreatedAt()).orElse(null)))
                    .collect(Cc.toL());
        });
    }


    @Override
    public OneOf<Boolean, Exception> trySaveNewString(KvKey key, String newValueProvider) {
        return trySaveNewStringAndRaw(key, new KvAllValues<>(newValueProvider, O.empty(), O.empty()));
    }

    @Override
    public OneOf<Boolean, Exception> trySaveNewStringAndRaw(KvKey key, KvAllValues<String> newValueProvider) {
        final String firstSaveId = ids.shortIdS();
        try {
            return withTableEnsure(key, table -> {
                final Key kk = toAwsKey(key);
                final DynKVItem item = new DynKVItem(kk.partitionKeyValue().s(),
                        kk.sortKeyValue().map($ -> $.s()).orElse("_"),
                        newValueProvider.getValue(),
                        newValueProvider.getRawValue().orElse(null),
                        null, null, null,
                        newValueProvider.getTtl().map($ -> times.toSec($)).orElse(null),
                        null, firstSaveId);

                table.putItem(item);
                return OneOf.left(true);
            });
        } catch (ConditionalCheckFailedException | DuplicateItemException e) {
            return withTableEnsure(key, table -> {
                final DynKVItem kvItem = privateItemGet(table, key);
                if (kvItem == null) {
                    //if something goes wrong and the item is not in there
                    return OneOf.right(e);
                } else if (Fu.equal(kvItem.getFirstSaveId(), firstSaveId)) {
                    //special case with DynamoDB when item is saved with success, but Dynamo returns exception that it does not
                    return OneOf.left(true);
                } else {
                    //if non from the above - the item is created by someone else
                    return OneOf.left(false);
                }
            });
        } catch (Exception e) {
            return OneOf.right(e);
        }
    }

    @Override
    public OneOf<O<String>, Exception> updateString(KvKeyWithDefault key, F1<String, O<String>> updater) {
        return updateStringAndRaw(key, av -> {
            final O<String> apply = updater.apply(av.getValue());
            return apply.map($ -> new KvAllValues<>($, O.empty(), O.empty()));
        }).mapLeft($ -> $.map(KvAllValues::getValue));
    }

    @Override
    public OneOf<O<KvAllValues<String>>, Exception> updateStringAndRaw(KvKeyWithDefault key,
            F1<KvAllValues<String>, O<KvAllValues<String>>> updater) {
        return withTableEnsure(key, table -> {
            int k = 1000;
            Key _key = toAwsKey(key);

            while (k-- >= 0) {
                try {
                    KvVersionedItemAll<String> raw = getOrCreateNewAllValues(table, key);
                    O<KvAllValues<String>> oupdatedValue = O.empty();
                    oupdatedValue = updater.apply(raw.getVals());
                    if (oupdatedValue.isEmpty()) {
                        return OneOf.left(O.empty());
                    }
                    final KvAllValues<String> updatedValue = oupdatedValue.get();
                    table.putItem(new DynKVItem(_key.partitionKeyValue().s(),
                            _key.sortKeyValue().map($ -> $.s()).orElse("_"),
                            updatedValue.getValue(),
                            updatedValue.getRawValue().orElse(null),
                            null, null, null,
                            updatedValue.getTtl().map($ -> times.toSec($)).orElse(null),
                            (Long) raw.getVersion(),
                            null));
                    return OneOf.left(oupdatedValue);
                } catch (ConditionalCheckFailedException
                        | ProvisionedThroughputExceededException
                        | RequestLimitExceededException e) {
                    async.sleep(500);
                    continue;
                } catch (Exception e) {
                    return OneOf.right(e);
                }
            }
            return OneOf.right(ConditionalCheckFailedException.create("Can't save kvKey:" + key, null));
        });
    }

    private KvVersionedItemAll<String> getOrCreateNewAllValues(DynamoDbTable<DynKVItem> table, KvKeyWithDefault outerKey) {
        return getRawVersionedAll(outerKey).orElseGet(() -> {
            trySaveNewStringAndRaw(outerKey, new KvAllValues<>(outerKey.getDefaultValue(), O.empty(), O.empty()));
            DynKVItem one = privateItemGet(table, outerKey);
            final O<ZonedDateTime> ttl = O.ofNull(one.getTtl()).map($ -> times.toZDT($));
            return new KvVersionedItemAll<>(outerKey,
                    new KvAllValues<>(one.getValue(), O.ofNull(one.getRawValue()), ttl),
                    ttl, null, one.getVersion());
        });
    }

    @Override
    public boolean tryLockOrRenew(KvLockOrRenewKey key, String whoLocks, O<Long> lockIsOldAfterOrElseRenew) {
        throw new NotImplementedException("No impl for DynamoDB yet");
    }

    @Override
    public void clearValue(KvKey key) {
        repeat.repeat(() -> withTableEnsureRun(key, table -> table.deleteItem(toAwsKey(key))), 10, 500);
    }

    @Override
    public void clearAll() {
        if (!appProfile.getProfile().isForProductionUsage()) {
            tableCache.values().stream()
                    .parallel()
                    .forEach($ -> dynaLowLvl.deleteTable(DeleteTableRequest.builder().tableName($.tableName()).build()));
        }
    }


    protected <T> T withTableEnsure(KvKey key, F1<DynamoDbTable<DynKVItem>, T> executor) {
        final DynamoDbTable<DynKVItem> table = getTable(key);
        for (int i = 0; i < 30; i++) {
            try {
                return executor.apply(table);
            } catch (ResourceNotFoundException e) {
                //AWS table not exist yet
                try {
                    table.createTable();
                } catch (ResourceInUseException tableAlreadyCreatedIgnoreIt) { }
                async.sleep(2000);
            }
        }
        throw DynamoDbException.builder().message("Table '" + table.tableName() + "' is unavailable").build();
    }

    protected void withTableEnsureRun(KvKey key, C1<DynamoDbTable<DynKVItem>> executor) {
        withTableEnsure(key, t -> {
            executor.accept(t);
            return null;
        });
    }

    protected final Map<String, DynamoDbTable<DynKVItem>> tableCache = new ConcurrentHashMap<>();

    protected DynamoDbTable<DynKVItem> getTable(KvKey key) {
        final String profilePrefix = endWith(conf.getTablePrefix(), "_");
        String tableName;
        final List<String> categories = key.categories();
        if (categories.size() == 0) {
            throw new IllegalArgumentException("KvKey provides no categories: " + key);
        } else if (categories.size() == 1) {
            tableName = "general_purpose_kv";
        } else {
            tableName = categories.get(0);
        }

        return tableCache
                .computeIfAbsent(profilePrefix + tableName, s -> dynaHighLvl.table(s, TableSchema.fromBean(DynKVItem.class)));
    }

    private final F2<String, String, Key> keyCreator =
            (hash, range) -> Key.builder().partitionValue(hash).sortValue(range).build();

    protected Key toAwsKey(KvKey k) {
        return toAwsKey(k, keyCreator);
    }

    protected Key toAwsKey(KvKey k, F2<String, String, Key> keyCreator) {
        final List<String> categories = k.categories();
        if (categories.size() == 0) {
            throw new IllegalArgumentException("KvKey provides no categories: " + keyCreator);
        } else if (categories.size() == 1) {
            return keyCreator.apply(categories.get(0), "_");
        } else if (categories.size() == 2) {
            return keyCreator.apply(categories.get(1), "_");
        } else {
            return keyCreator.apply(categories.get(1), categories.stream().skip(2).collect(joining("_")));
        }
    }

    static KvKey toKvKeyWithOtherKey2(KvKey baseKey, String key1, String key2) {
        final List<String> categories = baseKey.categories();
        final List<String> newCategories = Cc.l();
        if (!categories.contains(key1)) {
            throw new RuntimeException("Categories " + Cc.join(categories) + " not contain:" + key1 + " key2=" + key2);
        }
        for (String category : categories) {
            newCategories.add(category);
            if (Fu.equal(category, key1)) {
                break;
            }
        }
        newCategories.add(key2);
        return new KvKeyRaw(newCategories);
    }

    @NotNull
    private GetItemEnhancedRequest consistentGet(KvKey key) {
        return GetItemEnhancedRequest.builder().key(toAwsKey(key)).consistentRead(true).build();
    }

    private DynKVItem privateItemGet(DynamoDbTable<DynKVItem> table, KvKey outerKey) {
        return table.getItem(consistentGet(outerKey));
    }
}
