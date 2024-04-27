package jsk.gcl.agent.services;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2024 Core General
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
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import sk.services.CoreServicesRaw;
import sk.services.ICoreServices;
import sk.utils.functional.O;
import sk.utils.tuples.X1;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GcaInternalSyncedMetaTest {
    @Test
    @SneakyThrows
    void readOrUpdateObject() {
        ICoreServices core = new CoreServicesRaw();
        X1<String> txt = new X1<>();
        var meta = new GcaInternalSyncedMeta<>(core.json(), "?", A.class, new A()) {

            @Override
            protected void saveToDisc(String filePath, String text) {
                txt.set(text);
            }

            @Override
            protected O<String> loadFromDisc(String filePath) {
                return O.ofNull(txt.get());
            }
        };

        A[] toret = new A[1];
        meta.readOrUpdateObject(a -> {
            a.setA("B");
            a.setB(2);
            toret[0] = a;
        });

        A from = core.json().from(txt.get(), A.class);
        assertEquals(toret[0], from);
    }

    @Data
    public static class A implements Serializable {
        String a = "a";
        int b = 1;
        C c = new C();
    }

    @Data
    public static class C {
        int d = 5;
    }
}
