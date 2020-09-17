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

import lombok.Data;
import lombok.NoArgsConstructor;
import sk.services.kv.keys.KvKey;
import sk.services.kv.keys.KvKeyRaw;
import sk.utils.statics.Cc;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.List;

import static sk.utils.statics.Fu.equal;

@Embeddable
@Data
@NoArgsConstructor
public class KVItemId implements Serializable {
    public static final String DEFAULT = "DEFAULT";
    @Column(name = "key1")
    String key1;
    @Column(name = "key2")
    String key2;

    public KVItemId(KvKey key) {
        final List<String> categories = key.categories();
        if (categories.size() == 0) {
            key1 = DEFAULT;
            key2 = DEFAULT;
        } else if (categories.size() == 1) {
            key1 = DEFAULT;
            key2 = categories.get(0);
        } else if (categories.size() == 2) {
            key1 = categories.get(0);
            key2 = categories.get(1);
        } else {
            key1 = categories.get(0);
            key2 = Cc.join("_", categories.subList(1, categories.size()));
        }
    }

    public KvKey toKvKey() {
        if (equal(key1, DEFAULT) && equal(key2, DEFAULT)) {
            return new KvKeyRaw(Cc.l());
        } else if (equal(key1, DEFAULT)) {
            return new KvKeyRaw(Cc.l(key2));
        } else {
            return new KvKeyRaw(Cc.l(key1, key2));
        }
    }
}
