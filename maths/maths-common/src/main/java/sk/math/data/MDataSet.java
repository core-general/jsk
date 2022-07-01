package sk.math.data;

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

import lombok.Getter;
import sk.utils.minmax.MinMaxAvg;
import sk.utils.statics.Ar;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

@Getter
public class MDataSet {
    private String name;
    private String[] nameX;
    private String nameY;
    double[] y;
    double[][] x;

    public MDataSet(MDataSet base, MDataSet... datasets) {
        var baseY = DoubleStream.of(base.getY());
        for (MDataSet dataset : datasets) {
            baseY = DoubleStream.concat(baseY, DoubleStream.of(dataset.getY()));
        }
        y = baseY.toArray();

        var baseX = Stream.of(base.getX());
        for (MDataSet dataset : datasets) {
            baseX = Stream.concat(baseX, Stream.of(dataset.getX()));
        }
        x = baseX.toArray(double[][]::new);

        name = base.getName();
        nameX = base.getNameX();
        nameY = base.getNameY();
    }

    public MDataSet(double[] y, double[][] x) {
        this("NONAME", "Y", defaultXNames("X", x), y, x);
    }

    public MDataSet(String name, double[] y, double[][] x) {
        this(name, "Y", defaultXNames("X", x), y, x);
    }

    public MDataSet(String name, String nameY, String prefiX, double[] y, double[][] x) {
        this(name, nameY, defaultXNames(prefiX, x), y, x);
    }

    public MDataSet(String name, String nameY, String[] nameX, double[] y, double[][] x) {
        //region Validate
        if (y.length != x.length) {
            throw new RuntimeException(String.format("ys.length!=xs.length %d!=%d", y.length, x.length));
        }

        final long countOfXVars = Arrays.stream(x).map($ -> $.length).distinct().count();
        if (countOfXVars != 1) {
            throw new RuntimeException(String.format("countOfXVars!=1  %d!=1", countOfXVars));
        }
        //endregion

        this.name = name;
        this.nameX = nameX;
        this.nameY = nameY;
        this.y = y;
        this.x = x;
    }

    public MDataSet(double[] y) {
        this("NONAME", "Y", "X", y, Ar.getValuesIncrementedBy1(y.length));
    }

    public MDataSet(double[] y, double[] x) {
        this("NONAME", "Y", "X", y, x);
    }

    public MDataSet(String name, double[] y, double[] x) {
        this(name, "Y", "X", y, x);
    }

    public MDataSet(String name, String nameY, String nameX, double[] y, double[] x) {
        this(name, nameY, nameX, y, Arrays.stream(x).mapToObj($ -> new double[]{$}).toArray(double[][]::new));
    }

    public void eachPoint(MDataPointConsumer consumer) {
        for (int i = 0; i < y.length; i++) {
            consumer.consume(x[i], y[i]);
        }
    }

    public X2<MinMaxAvg[], MinMaxAvg> getLimits() {
        MinMaxAvg[] xM = Cc.fill(nameX.length, MinMaxAvg::new).toArray(MinMaxAvg[]::new);
        MinMaxAvg yM = new MinMaxAvg();
        eachPoint((x1, y1) -> {
            yM.add(y1);
            for (int i = 0; i < x1.length; i++) {
                xM[i].add(x1[i]);
            }
        });
        return X.x(xM, yM);
    }

    private static String[] defaultXNames(String prefix, double[][] x) {
        if (x.length > 0 && x[0].length > 1) {
            String[] names = new String[x[0].length];
            for (int i = 0; i < x[0].length; i++) {
                names[i] = prefix + i;
            }
            return names;
        } else {
            return new String[]{prefix};
        }
    }
}
