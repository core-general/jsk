package sk.services.kv.keys;

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

import org.junit.jupiter.api.Test;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;
import sk.utils.tuples.X1;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KvKeyEnumTest {

    @Test
    public void name() {
        X1<String> trickster = X.x(null);
        KvKeyEnum key = new KvKeyEnum() {
            @Override
            public String name() {
                return trickster.getI1();
            }

            @Override
            public String getDefaultValue() {
                return null;
            }
        };

        trickster.set("abc_def_gcs_gkt");
        assertEquals(Cc.join(key.categories()), "abc,def,gcs_gkt");

        trickster.set("abc_def_gcs");
        assertEquals(Cc.join(key.categories()), "abc,def,gcs");

        trickster.set("abc_def");
        assertEquals(Cc.join(key.categories()), "abc,def");

        trickster.set("abc");
        assertEquals(Cc.join(key.categories()), "abc");

        trickster.set("");
        assertEquals(Cc.join(key.categories()), "");
    }
}
