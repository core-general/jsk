package sk.services.kv;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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

import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.Value;
import sk.services.json.IJson;
import sk.services.kv.keys.KvKey;
import sk.services.kv.keys.KvKeyWithDefault;
import sk.services.kv.keys.KvLockOrRenewKey;
import sk.services.time.ITime;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.tuples.X1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor
public class IKvLocal4Test extends IKvStoreJsonBased implements IKvLimitedStore, IKvUnlimitedStore {
    private Map<Key, KvVersionedItemAll<String>> data = new HashMap<>();

    @Inject ITime times;

    public IKvLocal4Test(IJson json, ITime times) {
        super(json);
        this.times = times;
    }

    @Override
    public synchronized O<KvVersionedItem<String>> getRawVersioned(KvKeyWithDefault key) {
        return O.ofNull(data.get(key(key))).map($ -> $.toSimple());
    }

    @Override
    public synchronized O<KvVersionedItemAll<String>> getRawVersionedAll(KvKeyWithDefault key) {
        return O.ofNull(data.get(key(key)));
    }

    @Override
    public synchronized List<KvListItemAll<String>> getRawVersionedListBetweenCategories(KvKey baseKey,
            O<String> fromLastCategory,
            O<String> toLastCategory,
            int maxCount, boolean ascending) {
        return Ex.notImplemented();
    }

    @Override
    public synchronized OneOf<Boolean, Exception> trySaveNewStringAndRaw(KvKey key, KvAllValues<String> newValueProvider) {
        if (data.containsKey(key(key))) {
            return OneOf.left(true);
        } else {
            data.put(key(key), toVersionedAll(key, newValueProvider));
            return OneOf.left(false);
        }
    }


    @Override
    public synchronized OneOf<O<KvAllValues<String>>, Exception> updateStringAndRaw(KvKeyWithDefault key,
            F1<KvAllValues<String>, O<KvAllValues<String>>> updater) {
        X1<Boolean> valIsOld = new X1<>(false);

        final KvVersionedItemAll<String> val = Cc.computeAndApply(data, key(key),
                (k, old) -> {
                    final KvAllValues<String> oldVal =
                            old != null ? old.getVals() : new KvAllValues<>(key.getDefaultValue(), O.empty(), O.empty());
                    return updater.apply(oldVal)
                            .map($ -> toVersionedAll(key, $))
                            .orElseGet(() -> {
                                valIsOld.setI1(true);
                                return toVersionedAll(key, oldVal);
                            });
                }, () -> null);
        return valIsOld.get() ? OneOf.left(O.empty()) : OneOf.left(O.of(val.getVals()));
    }

    @Override
    public synchronized boolean tryLockOrRenew(KvLockOrRenewKey key, String whoLocks, O<Long> lockIsOldAfterOrElseRenew) {
        return Ex.notImplemented();
    }

    @Override
    public synchronized void clearValue(KvKey key) {
        data.remove(key(key));
    }

    @Override
    public synchronized void clearAll() {
        data.clear();
    }

    private static Key key(KvKey kk) {
        return new Key(kk);
    }

    @Override
    public void clearAll(O<List<KvKey>> except) {
        final List<Key> toPreserve = except.orElse(Cc.lEmpty()).stream().map(Key::new).collect(Collectors.toList());
        data = data.entrySet().stream()
                .filter($ -> toPreserve.stream().anyMatch($$ -> $.getKey().startsWith($$)))
                .collect(Cc.toMEntry());
    }

    @Value
    private static class Key {
        String key;

        public Key(KvKey key) {
            this.key = Cc.join(key.categories());
        }

        public boolean startsWith(Key other) {
            return key.startsWith(other.key);
        }
    }


    private KvVersionedItemAll<String> toVersionedAll(KvKey key, KvAllValues<String> newValueProvider) {
        return new KvVersionedItemAll<>(new KvKeyWithDefault() {
            @Override
            public String getDefaultValue() {
                return "NONE_VALUE";
            }

            @Override
            public List<String> categories() {
                return key.categories();
            }
        }, newValueProvider, O.ofNull(newValueProvider).flatMap(KvAllValues::getTtl), times.nowZ(), null);
    }
}
