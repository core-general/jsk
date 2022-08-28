package sk.services.ids;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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

import sk.services.ICore4Test;

public class IdsImplTest {
    static ICore4Test ic4t = new ICore4Test();
    static IIds ids = ic4t.ids();

    //public static void main(String[] args) {
    //    Map<String, Void> alreadyHave = new ConcurrentHashMap<>();
    //    AtomicLong haveCounter = new AtomicLong(0l);
    //    AtomicNotifier an = new AtomicNotifier(Integer.MAX_VALUE - 1, 100_000_000, s -> {
    //        System.out.println(s);
    //        System.out.println("have: " + haveCounter.get());
    //    });
    //
    //    IntStream.range(0, Integer.MAX_VALUE - 1).parallel().forEach(i -> {
    //        final String val = ids.unique(i + "", 1);
    //        Cc.compute(alreadyHave, val, (k, v) -> {
    //            haveCounter.incrementAndGet();
    //            return null;
    //        }, () -> null);
    //        an.incrementAndNotify();
    //    });
    //}
}
