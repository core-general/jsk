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

import org.junit.jupiter.api.Test;
import sk.services.kv.keys.KvKeyRaw;
import sk.utils.statics.Cc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static sk.aws.dynamo.DynKVStoreImpl.toKvKeyWithOtherKey2;

public class DynKVStoreImplTest {
    @Test
    public void toKvKeyWithOtherKey2_test() {
        assertEquals(Cc.join(toKvKeyWithOtherKey2(new KvKeyRaw(Cc.l("a", "b", "c")), "b", "d").categories()), "a,b,d");
        assertEquals(Cc.join(toKvKeyWithOtherKey2(new KvKeyRaw(Cc.l("a", "b", "c")), "c", "d").categories()), "a,b,c,d");
        assertThrows(RuntimeException.class, () -> toKvKeyWithOtherKey2(new KvKeyRaw(Cc.l("a", "b", "c")), "e", "f"));
    }
}
