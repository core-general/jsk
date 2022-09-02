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
import lombok.extern.log4j.Log4j2;
import org.apache.commons.math3.analysis.DifferentiableMultivariateVectorFunction;
import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.optimization.PointVectorValuePair;
import org.apache.commons.math3.optimization.general.LevenbergMarquardtOptimizer;
import org.jetbrains.annotations.NotNull;
import sk.math.data.MDataSet;
import sk.math.data.estimate.global.MGlobalOptimizer;
import sk.math.data.estimate.global.MGlobalRandomParamsOptimization;
import sk.math.data.func.MFuncImpl;
import sk.math.data.func.MFuncProto;
import sk.utils.functional.F2;
import sk.utils.functional.O;
import sk.utils.statics.Ar;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.DoubleStream;


@Log4j2
public class MLeastSquareFuncEstimator {
    @SneakyThrows
    public static <T extends MFuncProto> O<MOptimizeInfo<T>>
    funcParallelRandomSearch(MDataSet data, Class<T> funcCls) {
        final T protoFunc = funcCls.getConstructor().newInstance();
        return func(data, funcCls, 100_000, Duration.of(10, ChronoUnit.SECONDS),
                new MGlobalRandomParamsOptimization<T>(10000, -5, 5, protoFunc.paramCount(), true));
    }

    @SneakyThrows
    public static <T extends MFuncProto> O<MOptimizeInfo<T>>
    func(MDataSet data, Class<T> funcCls, int iterations, Duration maxTimePerLocalLocalization,
            MGlobalOptimizer<T> globalOptimizer) {
        return func(data, funcCls.getConstructor().newInstance(), iterations, maxTimePerLocalLocalization, globalOptimizer);
    }

    @SneakyThrows
    public static <T extends MFuncProto> O<MOptimizeInfo<T>>
    func(MDataSet data, T funcImpl, int iterations, Duration maxTimePerLocalLocalization, MGlobalOptimizer<T> globalOptimizer) {
        return func(data, funcImpl, iterations, maxTimePerLocalLocalization, globalOptimizer, (functionVal, target) -> {
            final double[] residuals = new double[target.length];
            for (int i = 0; i < target.length; i++) {
                residuals[i] = target[i] - functionVal[i];
            }
            return residuals;
        });
    }

    @SneakyThrows
    public static <T extends MFuncProto> O<MOptimizeInfo<T>>
    func(MDataSet data, T funcImpl, int iterations, Duration maxTimePerLocalLocalization, MGlobalOptimizer<T> globalOptimizer,
            F2<double[], double[], double[]> residualsComputations) {
        if (data.getX().length == 0) {
            throw new RuntimeException("NO DATA");
        }

        final T protoFunc = funcImpl;
        MFF mff = new MFF(data.getX(), protoFunc);

        ThreadLocal<Instant> currentPasStarted = new ThreadLocal<>();
        ThreadLocal<MOptimizeInfo<T>> bestPoint = new ThreadLocal<>();
        LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer(
                (iteration, previous, current) -> {
                    final Instant started = currentPasStarted.get();
                    final Instant now = Instant.now();
                    final Duration curDif = Duration.between(started, now);

                    final MOptimizeInfo<T> bestOptimizePass = bestPoint.get();
                    final MOptimizeInfo<T> newOptimizePass =
                            getOptimizeInfoO(data, protoFunc, current.getPoint(), current.getValueRef()).get();

                    if (bestOptimizePass == null ||
                            bestOptimizePass.getSquareRootError() > newOptimizePass.getSquareRootError()) {
                        bestPoint.set(newOptimizePass);
                    }

                    return iteration >= iterations - 1 || curDif.compareTo(maxTimePerLocalLocalization) > 0;
                }) {
            @Override
            protected double[] computeResiduals(double[] functionVal) {
                final double[] target = getTarget();
                if (functionVal.length != target.length) {
                    throw new DimensionMismatchException(target.length,
                            functionVal.length);
                }

                return residualsComputations.apply(functionVal, target);
            }
        };
        final O<MOptimizeInfo<T>> optimum = globalOptimizer
                .optimize(data, (initial) -> {
                    currentPasStarted.set(Instant.now());
                    bestPoint.remove();
                    return localOptimizationPass(initial, data, optimizer, mff, protoFunc, iterations);
                }).or(() -> O.ofNull(bestPoint.get()));
        return optimum;
    }

    private static <T extends MFuncProto> O<MOptimizeInfo<T>>
    localOptimizationPass(double[] startingPoint,
            MDataSet data,
            LevenbergMarquardtOptimizer optimizer,
            MFF mff, T protoFunc, int iterations
    ) {
        try {

            PointVectorValuePair optimum = optimizer.optimize(iterations,
                    mff,
                    data.getY(),
                    Ar.fill(data.getX().length, 1d),
                    startingPoint

            );

            return getOptimizeInfoO(data, protoFunc, optimum.getKey(), optimum.getValueRef());
        } catch (Exception e) {
            log.error("", e);
            return O.empty();
        }
    }

    @NotNull
    private static <T extends MFuncProto> O<MOptimizeInfo<T>> getOptimizeInfoO(MDataSet data, T protoFunc, double[] point,
            double[] valueRef) {
        double[] doubles = new double[data.getY().length];
        final MFuncImpl<T> func = new MFuncImpl<>(protoFunc, point);

        for (int i = 0; i < data.getY().length; i++) {
            doubles[i] = Math.pow((data.getY()[i] - (valueRef != null ? valueRef[i] : func.value(data.getX()[i]))), 2);
        }
        final double sum = DoubleStream.of(doubles).sum();
        return O.of(new MOptimizeInfo<>
                (func, Math.sqrt(sum) / data.getY().length));
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
