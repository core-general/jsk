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

import sk.utils.functional.O;
import sk.utils.random.MapRandom;
import sk.utils.statics.Cc;
import sk.utils.statics.St;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import static sk.utils.functional.O.ofNullable;

@SuppressWarnings("unused")
public interface IRand {
    Random getRandom();

    default <T> T rndDist(Map<T, Double> vals) {
        return MapRandom.rnd(this::rndDouble, vals);
    }

    default boolean rndBool(double trueProbability) {
        return MapRandom.rnd(this::rndDouble, Cc.m(true, trueProbability, false, 1 - trueProbability));
    }

    default int rndInt(int bound) {
        return getRandom().nextInt(bound);
    }

    default int rndInt(int minBound, int maxBound) {
        return minBound + rndInt(maxBound - minBound);
    }

    default double rndDouble() {
        return getRandom().nextDouble();
    }

    default <T> O<T> rndFromList(List<T> list) {
        return ofNullable(list.size() == 0 ? null : list.get(rndInt(list.size())));
    }

    default char rndChar(String str) {
        return str.charAt(rndInt(str.length()));
    }

    default String rndString(int fromIncluded, int toExcluded, String charSource) {
        return St.fromIntArray(IntStream.range(0, rndInt(fromIncluded, toExcluded))
                .map(a -> rndChar(charSource))
                .toArray());
    }

    default String rndString(int fromAndTo, String charSource) {
        return rndString(fromAndTo, fromAndTo + 1, charSource);
    }
}
