package sk.utils.javafixes;

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

import lombok.AllArgsConstructor;
import sk.utils.statics.St;

import java.util.BitSet;
import java.util.HashSet;

@AllArgsConstructor
/**
 *
 * Results of experiments for 1000 symbol text:
 * arrReplacer 0.01078ms
 * bitSetReplacer 0.01115ms
 * hashSetReplacer 0.01311ms
 *
 * Memory needs:
 * arrReplacer ~150kb (bytes = number of maximum code points)
 * bitSetReplacer ~18,5kb (bytes = number of maximum code points/8)
 * hashSetReplacer ~0.2kb (number of bytes in good examples)
 *
 * To conclude:
 * - the fastest is arrReplacer, but it eats the most memory (~4% faster than bitSetReplacer)
 * - bitSetReplacer is the optimal one for most cases because it's only ~4% slower than arrReplacer, but it needs 8 times less
 * memory
 * - hashSetReplacer eats memory proportional to good symbols (if you have eng alphabet + digits it's <200 bytes), but
 * is the slowest one, ~21% slower than arrReplacer
 *
 * Check main method down here
 */
public abstract class BadCharReplacer {
    final private String allGoodChars;
    final private BitManipulator bitManager;

    /**
     * Hash
     */
    public static BadCharReplacer hashSetReplacer(String allGoodChars) {
        return new BadCharReplacer(allGoodChars) {
            @Override
            protected BitManipulator initManipulator(int maxCodePoints) {
                return new HashSetManipulator(maxCodePoints);
            }
        };
    }

    public static BadCharReplacer bitSetReplacer(String allGoodChars) {
        return new BadCharReplacer(allGoodChars) {
            @Override
            protected BitManipulator initManipulator(int maxCodePoints) {
                return new BitSetManipulator(maxCodePoints);
            }
        };
    }

    public static BadCharReplacer arrReplacer(String allGoodChars) {
        return new BadCharReplacer(allGoodChars) {
            @Override
            protected BitManipulator initManipulator(int maxCodePoints) {
                return new ArrBitManipulator(maxCodePoints);
            }
        };
    }

    public BadCharReplacer(String allGoodChars) {
        this.allGoodChars = allGoodChars;
        bitManager = initManipulator(
                250_000/*todo update sometimes. Last tuned for Unicode 16.0 https://en.wikipedia.org/wiki/Unicode#Versions*/);
        St.forEachCodePoint(allGoodChars, bitManager::setBitTrue);
    }

    protected abstract BitManipulator initManipulator(int maxCodePoints);

    public String replaceChars(String toReplace, String badCharReplacement) {
        StringBuilder sb = new StringBuilder();
        St.forEachCodePoint(toReplace, c -> {
            if (bitManager.getBit(c)) {
                sb.appendCodePoint(c);
            } else {
                sb.append(badCharReplacement);
            }
        });
        return sb.toString();
    }

    protected interface BitManipulator {
        public void setBitTrue(int index);

        public boolean getBit(int index);
    }

    private static class BitSetManipulator implements BitManipulator {
        private final BitSet bits;

        public BitSetManipulator(int maxCodePoints) {
            bits = new BitSet(maxCodePoints);
        }

        @Override
        public void setBitTrue(int index) {
            bits.set(index);
        }

        @Override
        public boolean getBit(int index) {
            return bits.get(index);
        }
    }

    private static class ArrBitManipulator implements BitManipulator {
        private final boolean[] cp;

        public ArrBitManipulator(int maxCodePoints) {
            cp = new boolean[maxCodePoints];
        }

        @Override
        public void setBitTrue(int index) {
            cp[index] = true;
        }

        @Override
        public boolean getBit(int index) {
            return cp[index];
        }
    }

    private static class HashSetManipulator implements BitManipulator {
        private final HashSet<Integer> cp;

        public HashSetManipulator(int maxCodePoints) {
            cp = new HashSet<>();
        }

        @Override
        public void setBitTrue(int index) {
            cp.add(index);
        }

        @Override
        public boolean getBit(int index) {
            return cp.contains(index);
        }
    }


    //public static void main(String[] args) {
    //    final BadCharReplacer badCharReplacer = BadCharReplacer.bitSetReplacer(St.engENGDig + "❗", "🧿x");
    //    final String toReplace =
    //            "💎💎💎💎💎💎💎💎💎a🧿aaaa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa" +
    //                    "❗B☎B11❗💎💎a🧿aa❗💎💎a🧿aa💎💎💎💎💎💎💎💎💎a🧿aaaa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a" +
    //                    "🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗💎💎a🧿aa💎💎💎💎💎💎💎💎💎a🧿aaaa❗B☎B11" +
    //                    "❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗💎💎a" +
    //                    "🧿aa💎💎💎💎💎💎💎💎💎a🧿aaaa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B" +
    //                    "☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗💎💎a🧿aa💎💎💎💎💎💎💎💎💎a🧿aaaa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa" +
    //                    "❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗💎💎a🧿aa💎💎💎💎💎💎💎💎💎a" +
    //                    "🧿aaaa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a" +
    //                    "🧿aa❗💎💎a🧿aa💎💎💎💎💎💎💎💎💎a🧿aaaa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11" +
    //                    "❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗💎💎a🧿aa💎💎💎💎💎💎💎💎💎a🧿aaaa❗B☎B11❗💎💎a🧿aa❗B" +
    //                    "☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗💎💎a🧿aa" +
    //                    "💎💎💎💎💎💎💎💎💎a🧿aaaa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11" +
    //                    "❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗💎💎a🧿aa💎💎💎💎💎💎💎💎💎a🧿aaaa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B" +
    //                    "☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗B☎B11❗💎💎a🧿aa❗💎💎a🧿aa";
    //
    //    for (int i = 0; i < 1000; i++) {
    //        badCharReplacer.replaceChars(
    //                toReplace);
    //    }
    //    Ti.sleep(1000);
    //    final int maxCount = 10_000_000;
    //    SimpleNotifier an = new SimpleNotifier(maxCount, 1_000_000, System.out::println);
    //    System.out.println(Ti.naiveProfileMS(() -> {
    //        badCharReplacer.replaceChars(toReplace);
    //        an.incrementAndNotify();
    //    }, maxCount));
    //}
}
