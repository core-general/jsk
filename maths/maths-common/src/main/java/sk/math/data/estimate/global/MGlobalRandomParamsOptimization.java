package sk.math.data.estimate.global;

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

import sk.math.data.MDataSet;
import sk.math.data.estimate.MOptimizeInfo;
import sk.math.data.func.MFuncProto;
import sk.services.rand.IRand;
import sk.services.rand.RandImpl;
import sk.utils.functional.F1;
import sk.utils.functional.O;
import sk.utils.statics.Ar;
import sk.utils.statics.Cc;

import java.util.Comparator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class MGlobalRandomParamsOptimization<T extends MFuncProto> implements MGlobalOptimizer<T> {
    int maxCount;
    double[] mins;
    double[] maxes;
    int paramCount;
    boolean parallel;
    IRand rand;


    public MGlobalRandomParamsOptimization(int maxCount, double min, double max, int paramCount) {
        this(maxCount, Ar.fill(paramCount, min), Ar.fill(paramCount, max), false);
    }

    public MGlobalRandomParamsOptimization(int maxCount, double min, double max, int paramCount, boolean parallel) {
        this(maxCount, Ar.fill(paramCount, min), Ar.fill(paramCount, max), parallel);
    }

    public MGlobalRandomParamsOptimization(int maxCount, double[] mins, double[] maxes, boolean parallel) {
        this(maxCount, mins, maxes, parallel, new RandImpl());
    }

    public MGlobalRandomParamsOptimization(int maxCount, double[] mins, double[] maxes, boolean parallel, IRand rand) {
        if (mins.length != maxes.length) {
            throw new RuntimeException(String.format("mins.length!=maxes.length %d!=%d", mins.length, maxes.length));
        }
        this.maxCount = maxCount;
        this.mins = mins;
        this.maxes = maxes;
        this.paramCount = mins.length;
        this.parallel = parallel;
        this.rand = rand;
    }

    @Override
    public O<MOptimizeInfo<T>> optimize(MDataSet data,
            F1<double[], O<MOptimizeInfo<T>>> initialParamsToOptimizedInfo) {
        IntStream stream = IntStream.generate(() -> 0);
        if (parallel) {
            stream = stream.parallel();
        }
        var result = stream
                .mapToObj($ -> initialParamsToOptimizedInfo.apply(
                        IntStream.range(0, paramCount).mapToDouble((i) -> rand.rndDouble(mins[i], maxes[i])).limit(paramCount)
                                .toArray()
                ))
                .filter($ -> $.isPresent())
                .filter($ -> DoubleStream.of($.get().getOptimizedFunction().getParams()).allMatch(Double::isFinite))
                .map($ -> $.get())
                .limit(maxCount)
                .sorted(Comparator.comparing(MOptimizeInfo::getSquareRootError))
                .collect(Cc.toL());
        return Cc.first(result);
    }
}
