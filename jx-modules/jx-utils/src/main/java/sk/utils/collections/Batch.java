package sk.utils.collections;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2023 Core General
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

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import sk.utils.functional.F0;

import java.util.Collection;
import java.util.Map;

@AllArgsConstructor
@RequiredArgsConstructor
public class Batch<K, V> {
    private final Map<K, V> data;
    private F0<V> defaultValue = () -> null;

    public Collection<K> getKeys() {
        return data.keySet();
    }

    public V get(K key) {
        return getOrDefault(key, defaultValue);
    }

    public V getOrDefault(K key, F0<V> val) {
        V v = data.get(key);
        return v == null ? val.get() : v;
    }

    public V getOrDefault(K key, V val) {
        V v = data.get(key);
        return v == null ? val : v;
    }
}
