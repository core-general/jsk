package sk.utils.javafixes;

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

import org.junit.jupiter.api.Test;
import sk.utils.statics.St;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sk.utils.javafixes.Base62.decodeStr;
import static sk.utils.javafixes.Base62.encodeStr;

public class Base62Test {

    @Test
    public void generalTest() {
        String abc = encodeStr("abc");
        assertEquals(abc, "QmIN");
        assertEquals(decodeStr(abc), "abc");
        assertEquals(decodeStr(encodeStr("fdlgjwrljg3ojto3u034u0j340-j0jt2jt-29j4t-92j4t0-924j")),
                "fdlgjwrljg3ojto3u034u0j340-j0jt2jt-29j4t-92j4t0-924j");

        for (int i = 1; i < St.engENGDig.length(); i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < i; j++) {
                sb.append(St.engENGDig.charAt(j));
            }
            assertEquals(sb.toString(), decodeStr(encodeStr(sb.toString())));
        }
    }
}
