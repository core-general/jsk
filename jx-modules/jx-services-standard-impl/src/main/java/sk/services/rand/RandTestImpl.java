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
import lombok.experimental.Delegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sk.utils.paging.RingPicker;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class RandTestImpl extends RandImpl implements IRandSetter {

    private AtomicReference<RingPicker<Integer>> intSequence;
    private AtomicReference<RingPicker<Double>> doubleSequence;
    private AtomicReference<RingPicker<String>> stringSequence;
    private Random testRandom;

    @PostConstruct
    public RandTestImpl init() {
        intSequence = new AtomicReference<>(null);
        doubleSequence = new AtomicReference<>(null);
        stringSequence = new AtomicReference<>(null);
        testRandom = customRandom();
        return this;
    }

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

    @NotNull
    private Random customRandom() {
        return new Rand4Test(intSequence, doubleSequence);
    }

    @RequiredArgsConstructor
    private static class Rand4Test extends Random {
        @Delegate(excludes = Rand4TestExcludes.class) Random r = ThreadLocalRandom.current();

        final AtomicReference<RingPicker<Integer>> intSequence;
        final AtomicReference<RingPicker<Double>> doubleSequence;

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
