package sk.jfree;

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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import sk.math.data.MDataSet;
import sk.math.data.MDataSets;
import sk.math.data.estimate.MOptimizeInfo;
import sk.math.data.func.MFuncProto;
import sk.utils.functional.F2;
import sk.utils.functional.O;
import sk.utils.minmax.MinMaxAvg;
import sk.utils.statics.Cc;
import sk.utils.tuples.X2;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: Kivan
 * Date: 22.07.12
 * Time: 0:15
 * To change this template use File | Settings | File Templates.
 */
public class JGraphHelp {

    public static JFrame show(JFreeChart chart) {
        JFrame fr = new JFrame(chart.getTitle().getText());
        fr.setExtendedState(Frame.MAXIMIZED_BOTH);
        fr.setContentPane(panel(chart));
        fr.setTitle(chart.getTitle().getText());
        fr.setVisible(true);
        return fr;
    }

    public static ChartPanel panel(JFreeChart chart) {
        return new ChartPanel(chart);
    }

    public static BufferedImage image(int width, int height, JFreeChart chart) {
        return chart.createBufferedImage(width, height);
    }

    public static JFreeChart barChartX1(MDataSets datasets) {
        DefaultCategoryDataset collection = new DefaultCategoryDataset();

        for (MDataSet dataset : datasets.getDatasets()) {
            dataset.eachPoint((x, y) -> collection.addValue(y, dataset.getName(), (Double) x[0]));
        }

        return ChartFactory.createBarChart(
                datasets.getPlotName(),
                datasets.getXName()[0],
                datasets.getYName(),
                collection,
                PlotOrientation.VERTICAL, true, true, true);
    }

    public static JFreeChart lineChartX1(MDataSets datasets) {
        XYSeriesCollection collection = new XYSeriesCollection();

        for (MDataSet dataset : datasets.getDatasets()) {
            final XYSeries xySeries = new XYSeries(dataset.getName(), false, true);
            dataset.eachPoint((x, y) -> xySeries.add(x[0], y));
            collection.addSeries(xySeries);
        }

        return defaultRender(ChartFactory.createXYLineChart(
                datasets.getPlotName(),
                datasets.getXName()[0],
                datasets.getYName(),
                collection,
                PlotOrientation.VERTICAL, true, true, true));
    }

    public static JFreeChart scatterPlotX1(MDataSets datasets) {
        XYSeriesCollection collection = new XYSeriesCollection();

        for (MDataSet dataset : datasets.getDatasets()) {
            final XYSeries xySeries = new XYSeries(dataset.getName(), false, true);
            dataset.eachPoint((x, y) -> xySeries.add(x[0], y));
            collection.addSeries(xySeries);
        }

        return defaultRender(ChartFactory.createScatterPlot(
                datasets.getPlotName(),
                datasets.getXName()[0],
                datasets.getYName(),
                collection,
                PlotOrientation.VERTICAL, true, true, true));
    }

    public static JFreeChart uniPlotX1(MDataSets datasets, boolean[] lines, boolean[] points) {
        XYSeriesCollection collection = new XYSeriesCollection();

        for (MDataSet dataset : datasets.getDatasets()) {
            final XYSeries xySeries = new XYSeries(dataset.getName(), false, true);
            dataset.eachPoint((x, y) -> xySeries.add(x[0], y));
            collection.addSeries(xySeries);
        }

        return defaultRender(createUniPlot(
                datasets.getPlotName(),
                datasets.getXName()[0],
                datasets.getYName(),
                collection, lines, points,
                PlotOrientation.VERTICAL, true, true, true));
    }


    public static <T extends MFuncProto> JFreeChart functionAndDataSets(MDataSet dataset,
            List<Class<? extends MFuncProto>> functions,
            double functionDrawStep,
            F2<MDataSet, Class<T>, O<MOptimizeInfo<T>>> functionProducer) {
        final X2<MinMaxAvg[], MinMaxAvg> limits = dataset.getLimits();
        final List<MDataSet> datasets = functions.stream()
                .map($ -> functionProducer.apply(dataset, (Class<T>) $))
                .map($ -> {
                    final MinMaxAvg minMaxX = limits.i1()[0];
                    List<Double> xx = Cc.l();
                    List<Double> yy = Cc.l();
                    var zvals = (minMaxX.getMax() - minMaxX.getMin()) * 0.1;
                    for (double x = minMaxX.getMin() - zvals; x < minMaxX.getMax() + zvals; x = x + functionDrawStep) {
                        final double y = $.get().getOptimizedFunction().value(new double[]{x});
                        yy.add(y);
                        xx.add(x);
                    }
                    return new MDataSet(
                            $.get().getOptimizedFunction().getProtoClass().getSimpleName() +
                                    String.format(" err=%.2f", $.get().getSquareRootError()),
                            yy.stream().mapToDouble(y -> y).toArray(),
                            xx.stream().map(x -> new double[]{x}).toArray(double[][]::new)
                    );
                }).collect(Collectors.toList());

        datasets.add(0, dataset);

        boolean[] lines = new boolean[datasets.size()];
        Arrays.fill(lines, true);
        lines[0] = false;

        boolean[] dots = new boolean[datasets.size()];
        dots[0] = true;

        return defaultRender(uniPlotX1(
                new MDataSets(datasets),
                lines, dots
        ));
    }

    private static JFreeChart defaultRender(JFreeChart chart) {
        try {
            var render = (AbstractRenderer) chart.getXYPlot().getRenderer();
            render.setAutoPopulateSeriesStroke(false);
            render.setDefaultStroke(new BasicStroke(4));
            return chart;
        } catch (Exception e) {
            return chart;
        }
    }

    private static JFreeChart createUniPlot(String title, String xAxisLabel, String yAxisLabel,
            XYDataset dataset, boolean[] lines, boolean[] points,
            PlotOrientation orientation, boolean legend, boolean tooltips, boolean urls) {
        if (orientation == null) {
            throw new IllegalArgumentException("Null 'orientation' argument.");
        }
        NumberAxis xAxis = new NumberAxis(xAxisLabel);
        xAxis.setAutoRangeIncludesZero(false);
        NumberAxis yAxis = new NumberAxis(yAxisLabel);
        yAxis.setAutoRangeIncludesZero(false);
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, null);
        XYToolTipGenerator toolTipGenerator = null;
        if (tooltips) {
            toolTipGenerator = new StandardXYToolTipGenerator();
        }
        XYURLGenerator urlGenerator = null;
        if (urls) {
            urlGenerator = new StandardXYURLGenerator();
        }
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);

        if (dataset instanceof XYSeriesCollection) {
            XYSeriesCollection xys = (XYSeriesCollection) dataset;
            for (int i = 0; i < xys.getSeriesCount(); i++) {
                renderer.setSeriesLinesVisible(i, lines[i]);
                renderer.setSeriesShapesVisible(i, points[i]);
            }
        }

        renderer.setDefaultToolTipGenerator(toolTipGenerator);
        renderer.setURLGenerator(urlGenerator);
        plot.setRenderer(renderer);
        plot.setOrientation(orientation);
        return new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
    }
}
