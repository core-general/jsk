package jsk.spark;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import sk.services.CoreServicesRaw;
import sk.services.json.IJson;
import sk.utils.functional.O;

public class RecordTest {
    public static void main(String[] args) {
        IJson json = CoreServicesRaw.services().json();
        X x = new X(4, "6", null);
        final String to = json.to(x);
        final X from = json.from(to, X.class);

        Y y = new Y(x, new Y(from, null, "YY", O.empty()), "Y", O.of("5"));
        final String yy = json.to(y);
        final Y from1 = json.from(yy, Y.class);
    }

    public record X(int a, String b, String c) {
        public X {
            if (a > b.length()) {
                throw new RuntimeException();
            }
        }
    }

    public record Y(X a, Y b, String c, O<String> oo) {}
}
