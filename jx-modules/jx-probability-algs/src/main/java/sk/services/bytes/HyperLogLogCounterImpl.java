package sk.services.bytes;

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

import net.agkn.hll.HLL;
import sk.services.bytes.impl.MurMurHash3;

public class HyperLogLogCounterImpl extends ICountAllElements {
    private HLL hll;

    public HyperLogLogCounterImpl(int log2m, int registerWidth) {
        hll = new HLL(log2m, registerWidth);
    }

    public HyperLogLogCounterImpl(byte[] serialized) {
        super(serialized);
    }

    @Override
    public void addElement(byte[] element) {
        final long hash = MurMurHash3.hash_x86_32(element, element.length, 0);
        hll.addRaw(hash);
    }

    @Override
    public long getElementCount() {
        return hll.cardinality();
    }

    @Override
    public byte[] serialize() {
        return hll.toBytes();
    }

    @Override
    protected void deSerialize(byte[] data) {
        hll = HLL.fromBytes(data);
    }

    //public static void main(String[] args) {
    //    List<X2<String, X2<Integer, Integer>>> hllStats = Cc.l();
    //    final int C = 10000;
    //    for (int firstParam = 8; firstParam < 30; firstParam++) {
    //        for (int secondParam = 5; secondParam < 8; secondParam++) {
    //            try {
    //                final HyperLogLogCounterImpl hll =
    //                        new HyperLogLogCounterImpl(firstParam/*log2m*/, secondParam/*registerWidth*/);
    //                for (int i = 0; i < C; i++) {
    //                    hll.addElement(UUID.randomUUID());
    //                }
    //
    //                final byte[] bytes = hll.serialize();
    //                final long cardinality = hll.getElementCount();
    //                hllStats.add(X.x(firstParam + "_" + secondParam, X.x(bytes.length, (int) ((cardinality - C) * 100f / C))));
    //            } catch (Exception e) {
    //                System.out.println(firstParam + "_" + secondParam + " ERROR");
    //                e.printStackTrace();
    //            }
    //        }
    //    }
    //
    //    System.out.println(Cc.join("\n", Cc.sort(hllStats, Comparator.comparing($ -> $.i2().i1()))));
    //}
}
