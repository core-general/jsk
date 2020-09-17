package sk.services.kv;

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

import sk.services.kv.keys.KvKey;
import sk.services.kv.keys.KvKeyWithDefault;
import sk.services.kv.keys.KvLockOrRenewKey;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.javafixes.TypeWrap;
import sk.utils.statics.Ma;

import java.util.List;
import java.util.Objects;

public interface IKvStore {
    public <T> String objectToString(T object);

    public <T> T objectFromString(String object, TypeWrap<T> cls);


    public O<KvVersionedItem<String>> getRawVersioned(KvKeyWithDefault key);

    public O<KvVersionedItemAll<String>> getRawVersionedAll(KvKeyWithDefault key);

    /** Get items with last category between fromLastCategory and toLastCategory sorted with maximum */
    public List<KvListItemAll<String>> getRawVersionedListBetweenCategories(KvKey baseKey,
            O<String> fromLastCategory, O<String> toLastCategory, int maxCount, boolean ascending);


    /**
     * @return true if save is ok, false if there is existing value
     */
    public OneOf<Boolean, Exception> trySaveNewString(KvKey key, String newValueProvider);

    /**
     * @return true if save is ok, false if there is existing value
     */
    public OneOf<Boolean, Exception> trySaveNewStringAndRaw(KvKey key, KvAllValues<String> newValueProvider);


    public OneOf<O<String>, Exception> updateString(KvKeyWithDefault key, F1<String, O<String>> updater);

    public OneOf<O<KvAllValues<String>>, Exception> updateStringAndRaw(KvKeyWithDefault key,
            F1<KvAllValues<String>, O<KvAllValues<String>>> updater);


    boolean tryLockOrRenew(KvLockOrRenewKey key, String whoLocks, O<Long> lockIsOldAfterOrElseRenew);


    void clearValue(KvKey key);


    void clearAll();

    //region Read
    public default String getAsString(KvKeyWithDefault key) {
        O<KvVersionedItem<String>> rawVersioned = getRawVersioned(key);
        if (!rawVersioned.isPresent()) {
            newKeyOrNothing(key);
            rawVersioned = getRawVersioned(key);
        }
        return rawVersioned.get().getValue();
    }

    public default KvAllValues<String> getAsStringWithRaw(KvKeyWithDefault key) {
        O<KvVersionedItemAll<String>> rawVersioned = getRawVersionedAll(key);
        if (!rawVersioned.isPresent()) {
            newKeyOrNothing(key);
            rawVersioned = getRawVersionedAll(key);
        }
        return rawVersioned.get().getVals();
    }

    public default <T> T getAsObject(KvKeyWithDefault key, Class<T> cls) {
        return getAsObject(key, TypeWrap.simple(cls));
    }

    public default <T> KvAllValues<T> getAsObjectWithRaw(KvKeyWithDefault key, Class<T> cls) {
        return getAsStringWithRaw(key).map($ -> objectFromString($, TypeWrap.simple(cls)));
    }

    public default <T> T getAsObject(KvKeyWithDefault key, TypeWrap<T> cls) {
        return objectFromString(getAsString(key), cls);
    }

    public default Boolean getAsBool(KvKeyWithDefault key) {
        return Boolean.parseBoolean(getAsString(key));
    }

    public default Long getAsLong(KvKeyWithDefault key) throws NumberFormatException {
        return Long.parseLong(getAsString(key));
    }

    public default Integer getAsInt(KvKeyWithDefault key) throws NumberFormatException {
        return Integer.parseInt(getAsString(key));
    }

    public default Double getAsDouble(KvKeyWithDefault key) throws NumberFormatException {
        return Double.parseDouble(getAsString(key));
    }

    public default Float getAsFloat(KvKeyWithDefault key) throws NumberFormatException {
        return Float.parseFloat(getAsString(key));
    }
    //endregion

    //region Update
    public default <T> OneOf<O<T>, Exception> updateObject(KvKeyWithDefault key, TypeWrap<T> cls, F1<T, O<T>> updater) {
        return updateString(key, s -> updater.apply(objectFromString(s, cls)).map(this::objectToString))
                .mapLeft($ -> $.map($$ -> objectFromString($$, cls)));
    }

    public default <T> OneOf<O<T>, Exception> updateObject(KvKeyWithDefault key, Class<T> cls, F1<T, O<T>> updater) {
        return updateObject(key, TypeWrap.simple(cls), updater);
    }

    public default <T> OneOf<O<KvAllValues<T>>, Exception> updateObjectAndRaw(KvKeyWithDefault key, Class<T> valueCls,
            F1<KvAllValues<T>, O<KvAllValues<T>>> updater) {
        return updateObjectAndRaw(key, TypeWrap.simple(valueCls), updater);
    }

    public default <T> OneOf<O<KvAllValues<T>>, Exception> updateObjectAndRaw(KvKeyWithDefault key, TypeWrap<T> cls,
            F1<KvAllValues<T>, O<KvAllValues<T>>> updater) {
        return updateStringAndRaw(key, s -> updater
                .apply(s.map($ -> objectFromString($, cls)))
                .map($ -> $.map(this::objectToString))
        ).mapLeft($ -> $.map(x -> x.map(xx -> objectFromString(xx, cls))));
    }

    public default OneOf<O<Boolean>, Exception> updateBool(KvKeyWithDefault key, F1<Boolean, O<Boolean>> updater) {
        return updateString(key, s -> updater.apply(Boolean.parseBoolean(s)).map(Objects::toString))
                .mapLeft(s -> s.map(Ma::pb));
    }

    public default OneOf<O<Long>, Exception> updateLong(KvKeyWithDefault key, F1<Long, O<Long>> updater) {
        return updateString(key, s -> updater.apply(Long.parseLong(s)).map(Objects::toString))
                .mapLeft(s -> s.map(Ma::pl));
    }

    public default OneOf<O<Integer>, Exception> updateInt(KvKeyWithDefault key, F1<Integer, O<Integer>> updater) {
        return updateString(key, s -> updater.apply(Integer.parseInt(s)).map(Objects::toString))
                .mapLeft(s -> s.map(Ma::pi));
    }

    public default OneOf<O<Double>, Exception> updateDouble(KvKeyWithDefault key, F1<Double, O<Double>> updater) {
        return updateString(key, s -> updater.apply(Double.parseDouble(s)).map(Objects::toString))
                .mapLeft(s -> s.map(Ma::pd));
    }

    public default OneOf<O<Float>, Exception> updateFloat(KvKeyWithDefault key, F1<Float, O<Float>> updater) {
        return updateString(key, s -> updater.apply(Float.parseFloat(s)).map(Objects::toString))
                .mapLeft(s -> s.map(Ma::pf));
    }
    //endregion

    //region Try save
    public default <T> OneOf<Boolean, Exception> trySaveNewObject(KvKeyWithDefault key, T newValueProvider) {
        return trySaveNewString(key, objectToString(newValueProvider));
    }

    public default <T> OneOf<Boolean, Exception> trySaveNewObjectAndRaw(KvKeyWithDefault key, KvAllValues<T> newValueProvider) {
        return trySaveNewStringAndRaw(key, newValueProvider.map($ -> objectToString($)));
    }

    public default <T> OneOf<Boolean, Exception> trySaveNewBool(KvKeyWithDefault key, boolean newValueProvider) {
        return trySaveNewString(key, newValueProvider + "");
    }

    public default <T> OneOf<Boolean, Exception> trySaveNewLong(KvKeyWithDefault key, long newValueProvider) {
        return trySaveNewString(key, newValueProvider + "");
    }

    public default <T> OneOf<Boolean, Exception> trySaveNewInt(KvKeyWithDefault key, int newValueProvider) {
        return trySaveNewString(key, newValueProvider + "");
    }

    public default <T> OneOf<Boolean, Exception> trySaveNewDouble(KvKeyWithDefault key, double newValueProvider) {
        return trySaveNewString(key, newValueProvider + "");
    }

    public default <T> OneOf<Boolean, Exception> trySaveNewFloat(KvKeyWithDefault key, float newValueProvider) {
        return trySaveNewString(key, newValueProvider + "");
    }
    //endregion

    default void newKeyOrNothing(KvKeyWithDefault key) {
        updateString(key, $ -> {
            if ($ != null) {
                return O.empty();
            } else {
                return O.of(key.getDefaultValue());
            }
        });
    }

}
