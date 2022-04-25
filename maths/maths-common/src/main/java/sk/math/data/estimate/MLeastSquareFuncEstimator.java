package sk.math.data.estimate;

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

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.math3.analysis.DifferentiableMultivariateVectorFunction;
import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.optimization.PointVectorValuePair;
import org.apache.commons.math3.optimization.general.AbstractLeastSquaresOptimizer;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;
import sk.math.data.MDataSet;
import sk.math.data.estimate.global.MGlobalOptimizer;
import sk.math.data.estimate.global.MGlobalRandomParamsOptimization;
import sk.math.data.func.MFuncImpl;
import sk.math.data.func.MFuncProto;
import sk.utils.functional.O;
import sk.utils.statics.Ar;

import java.util.stream.DoubleStream;


public class MLeastSquareFuncEstimator {
    @SneakyThrows
    public static <T extends MFuncProto> O<MOptimizeInfo<T>>
    funcParallelRandomSearch(MDataSet data, Class<T> funcCls) {
        final T protoFunc = funcCls.getConstructor().newInstance();
        return func(data, funcCls, 100_000, new MGlobalRandomParamsOptimization<T>(10000, -5, 5, protoFunc.paramCount(), true));
    }

    @SneakyThrows
    public static <T extends MFuncProto> O<MOptimizeInfo<T>>
    func(MDataSet data, Class<T> funcCls, int iterations, MGlobalOptimizer<T> globalOptimizer) {
        if (data.getX().length == 0) {
            throw new RuntimeException("NO DATA");
        }

        final T protoFunc = funcCls.getConstructor().newInstance();
        MFF mff = new MFF(data.getX(), protoFunc);
        AbstractLeastSquaresOptimizer optimizer = new LevenbergMarquardtOptimizer();
        final O<MOptimizeInfo<T>> optimum = globalOptimizer
                .optimize(data, (initial) -> localOptimizationPass(initial, data, optimizer, mff, protoFunc, iterations));
        return optimum;
    }

    private static <T extends MFuncProto> O<MOptimizeInfo<T>>
    localOptimizationPass(double[] startingPoint,
            MDataSet data,
            AbstractLeastSquaresOptimizer optimizer,
            MFF mff, T protoFunc, int iterations
    ) {
        try {

            PointVectorValuePair optimum = optimizer.optimize(iterations,
                    mff,
                    data.getY(),
                    Ar.fill(data.getX().length, 1d),
                    startingPoint
            );

            double[] doubles = new double[data.getY().length];
            for (int i = 0; i < data.getY().length; i++) {
                doubles[i] = Math.pow((data.getY()[i] - optimum.getValueRef()[i]), 2);
            }
            final double sum = DoubleStream.of(doubles).sum();
            return O.of(new MOptimizeInfo<>
                    (new MFuncImpl<>(protoFunc, optimum.getKey()), Math.sqrt(sum) / data.getY().length));
        } catch (Exception e) {
            return O.empty();
        }
    }

    @AllArgsConstructor
    private static class MFF implements DifferentiableMultivariateVectorFunction {
        private final double[][] x;
        private final MFuncProto proto;

        @Override
        public double[] value(double[] params) throws IllegalArgumentException {
            double[] values = new double[x.length];
            for (int i = 0; i < values.length; ++i) {
                values[i] = proto.value(x[i], params);
            }
            return values;
        }

        @Override
        public MultivariateMatrixFunction jacobian() {
            return params -> {
                double[][] jacobian = new double[x.length][params.length];
                for (int i = 0; i < jacobian.length; ++i) {
                    final double[] jacob = proto.jacobian(x[i], params);
                    jacobian[i] = jacob;
                }
                return jacobian;
            };
        }
    }
}
