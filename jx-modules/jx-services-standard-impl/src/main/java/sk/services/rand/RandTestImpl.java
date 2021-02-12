package sk.services.rand;

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

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import sk.utils.paging.RingPicker;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@SuppressWarnings("unused")
public class RandTestImpl extends RandImpl implements IRandSetter {
    private final AtomicReference<RingPicker<Integer>> intSequence = new AtomicReference<>(null);
    private final AtomicReference<RingPicker<Double>> doubleSequence = new AtomicReference<>(null);
    private final AtomicReference<RingPicker<String>> stringSequence = new AtomicReference<>(null);
    private final Random testRandom = new Rand4Test(intSequence, doubleSequence);

    @Override
    public Random getRandom() {
        return testRandom;
    }


    @Override
    public void setIntSequence(@Nullable List<Integer> randomSequence) {
        if (randomSequence == null || randomSequence.size() == 0) {
            intSequence.set(null);
        } else {
            intSequence.set(RingPicker.create(randomSequence, 0));
        }
    }

    @Override
    public void setDoubleSequence(@Nullable List<Double> randomSequence) {
        if (randomSequence == null || randomSequence.size() == 0) {
            doubleSequence.set(null);
        } else {
            doubleSequence.set(RingPicker.create(randomSequence, 0));
        }
    }

    @Override
    public void setStringSequence(@Nullable List<String> randomSequence) {
        if (randomSequence == null || randomSequence.size() == 0) {
            stringSequence.set(null);
        } else {
            stringSequence.set(RingPicker.create(randomSequence, 0));
        }
    }

    @Override
    public String rndString(int fromIncluded, int toExcluded, String charSource) {
        return custom(() -> stringSequence.get(), () -> super.rndString(fromIncluded, toExcluded, charSource));
    }

    @Override
    public String rndString(int fromAndTo, String charSource) {
        return custom(() -> stringSequence.get(), () -> super.rndString(fromAndTo, charSource));
    }


    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private static <T> T custom(Supplier<RingPicker<T>> rpSup, Supplier<T> orElse) {
        RingPicker<T> rp = rpSup.get();
        if (rp != null && rp.get().isPresent()) {
            synchronized (rp) {
                T t = rp.get().get();
                rp.nextStop();
                return t;
            }
        } else {
            return orElse.get();
        }
    }

    @RequiredArgsConstructor
    private static class Rand4Test extends Random {
        Random r;

        final AtomicReference<RingPicker<Integer>> intSequence;
        final AtomicReference<RingPicker<Double>> doubleSequence;

        public Random getR() {
            if (r == null) {
                r = ThreadLocalRandom.current();
            }
            return r;
        }

        public void setSeed(long seed) {}

        public void nextBytes(byte[] bytes) {getR().nextBytes(bytes);}

        public int nextInt() {return getR().nextInt();}

        public long nextLong() {return getR().nextLong();}

        public boolean nextBoolean() {return getR().nextBoolean();}

        public float nextFloat() {return getR().nextFloat();}

        public double nextGaussian() {return getR().nextGaussian();}

        public IntStream ints(long streamSize) {return getR().ints(streamSize);}

        public IntStream ints() {return getR().ints();}

        public IntStream ints(long streamSize, int randomNumberOrigin, int randomNumberBound) {
            return getR().ints(streamSize, randomNumberOrigin, randomNumberBound);
        }

        public IntStream ints(int randomNumberOrigin, int randomNumberBound) {
            return getR().ints(randomNumberOrigin, randomNumberBound);
        }

        public LongStream longs(long streamSize) {return getR().longs(streamSize);}

        public LongStream longs() {return getR().longs();}

        public LongStream longs(long streamSize, long randomNumberOrigin, long randomNumberBound) {
            return getR().longs(streamSize, randomNumberOrigin, randomNumberBound);
        }

        public LongStream longs(long randomNumberOrigin, long randomNumberBound) {
            return getR().longs(randomNumberOrigin, randomNumberBound);
        }

        public DoubleStream doubles(long streamSize) {return getR().doubles(streamSize);}

        public DoubleStream doubles() {return getR().doubles();}

        public DoubleStream doubles(long streamSize, double randomNumberOrigin, double randomNumberBound) {
            return getR().doubles(streamSize, randomNumberOrigin, randomNumberBound);
        }

        public DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
            return getR().doubles(randomNumberOrigin, randomNumberBound);
        }

        @SuppressWarnings("unused")
        private interface Rand4TestExcludes {
            int nextInt(int bound);

            double nextDouble();
        }

        @Override
        public int nextInt(int bound) {
            return custom(intSequence::get, () -> ThreadLocalRandom.current().nextInt(bound));
        }

        @Override
        public double nextDouble() {
            return custom(doubleSequence::get, () -> ThreadLocalRandom.current().nextDouble());
        }
    }
}
