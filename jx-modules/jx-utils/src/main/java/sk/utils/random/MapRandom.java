package sk.utils.random;

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

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

@SuppressWarnings({"WeakerAccess", "unused"})
public class MapRandom<T> {
    final private Map<T, Double> randomDescription;
    final private DoubleSupplier randomStream;

    final private double df;

    public MapRandom(DoubleSupplier randomStream, Map<T, Double> randomDescription) {
        if (randomDescription.size() < 1) {
            throw new RuntimeException("randomDescription size must be > 0");
        }
        this.randomStream = randomStream;
        this.randomDescription = new HashMap<>(randomDescription);
        df = 1.0d / this.randomDescription.values().stream().mapToDouble(v -> v).sum();
    }

    public T getNext() {
        double rand = randomStream.getAsDouble();
        double tempDist = 0;
        for (Map.Entry<T, Double> dd : randomDescription.entrySet()) {
            tempDist += dd.getValue();
            if (rand / df <= tempDist) {
                return dd.getKey();
            }
        }
        return randomDescription.keySet().iterator().next();
    }

    public static <T> T rnd(DoubleSupplier randomizer, Map<T, Double> randomDescription) {
        return new MapRandom<>(randomizer, randomDescription).getNext();
    }

    public MapRandom(Map<T, Double> probabilities) {
        this(Math::random, probabilities);
    }
}
