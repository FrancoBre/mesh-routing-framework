package org.ungs.core.observability.metrics.impl.loadvsavg;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.observability.events.LoadLevelUpdatedEvent;
import org.ungs.core.observability.metrics.api.ComparisonRenderer;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.util.Tuple;
import org.ungs.util.Tuple3;

public final class AvgDeliveryTimeVsLoadLevelComparisonRenderer
    implements ComparisonRenderer<
        List<Tuple3<Double, Double, LoadLevelUpdatedEvent.LoadLevelTrend>>> {

  @Override
  public void renderComparison(
      Path out,
      SimulationConfigContext cfg,
      Map<AlgorithmType, List<Tuple3<Double, Double, LoadLevelUpdatedEvent.LoadLevelTrend>>>
          dataByAlgo) {

    try {
      Files.createDirectories(out);

      XYChart chart =
          new XYChartBuilder()
              .width(900)
              .height(600)
              .title("Avg Delivery Time vs Load Level (Rising vs Falling)")
              .xAxisTitle("Load Level (L)")
              .yAxisTitle("Average Delivery Time (moving avg)")
              .build();

      var styler = chart.getStyler();
      styler.setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
      styler.setChartBackgroundColor(Color.WHITE);
      styler.setPlotBackgroundColor(Color.WHITE);
      styler.setPlotGridLinesVisible(true);
      styler.setPlotGridLinesColor(new Color(220, 220, 220));
      styler.setLegendPosition(Styler.LegendPosition.OutsideE);
      styler.setLegendBorderColor(Color.WHITE);
      styler.setMarkerSize(4);

      Color[] colors = {
        new Color(40, 90, 160), // blue
        new Color(230, 150, 60), // orange
        new Color(70, 140, 90), // green
        new Color(150, 80, 160), // purple
        new Color(120, 120, 120) // gray
      };

      Map<AlgorithmType, List<Tuple3<Double, Double, LoadLevelUpdatedEvent.LoadLevelTrend>>>
          ordered =
              (dataByAlgo instanceof LinkedHashMap<?, ?>)
                  ? dataByAlgo
                  : new LinkedHashMap<>(dataByAlgo);

      int idx = 0;
      for (var entry : ordered.entrySet()) {
        AlgorithmType algo = entry.getKey();
        List<Tuple3<Double, Double, LoadLevelUpdatedEvent.LoadLevelTrend>> points =
            entry.getValue();
        if (points == null || points.isEmpty()) continue;

        List<Tuple<Double, Double>> rising = new ArrayList<>();
        List<Tuple<Double, Double>> falling = new ArrayList<>();

        for (var p : points) {
          double x = p.first();
          double y = p.second();
          var trend = p.third();

          if (trend == LoadLevelUpdatedEvent.LoadLevelTrend.RISING) {
            rising.add(new Tuple<>(x, y));
          } else if (trend == LoadLevelUpdatedEvent.LoadLevelTrend.FALLING) {
            falling.add(new Tuple<>(x, y));
          }
        }

        double binSize = 0.02;
        List<Tuple<Double, Double>> risingB = binAndAveragePreserveOrder(rising, binSize);
        List<Tuple<Double, Double>> fallingB = binAndAveragePreserveOrder(falling, binSize);

        fallingB.sort(Comparator.comparingDouble(Tuple::getFirst));
        risingB.sort(Comparator.comparingDouble(Tuple::getFirst));

        addSeries(chart, algo.name() + " (RISING)", risingB, colors[idx % colors.length]);
        addSeries(chart, algo.name() + " (FALLING)", fallingB, colors[idx % colors.length]);

        idx++;
      }

      Path file = out.resolve("avg_delivery_time_vs_load_level_comparison.png");
      BitmapEncoder.saveBitmap(chart, file.toString(), BitmapEncoder.BitmapFormat.PNG);

    } catch (IOException e) {
      throw new RuntimeException("Failed to render avg vs load-level comparison", e);
    }
  }

  private static void addSeries(
      XYChart chart, String name, List<Tuple<Double, Double>> pts, Color color) {

    if (pts == null || pts.isEmpty()) return;

    double[] x = pts.stream().mapToDouble(Tuple::getFirst).toArray();
    double[] y = pts.stream().mapToDouble(Tuple::getSecond).toArray();

    XYSeries s = chart.addSeries(name, x, y);

    s.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
    s.setMarker(SeriesMarkers.CIRCLE);
    s.setMarkerColor(color);

    s.setLineStyle(SeriesLines.NONE);
  }

  private static List<Tuple<Double, Double>> binAndAveragePreserveOrder(
      List<Tuple<Double, Double>> data, double binSize) {

    if (binSize <= 0) throw new IllegalArgumentException("binSize must be > 0");
    if (data == null || data.isEmpty()) return List.of();

    Map<Long, DoubleSummaryStatistics> stats = new LinkedHashMap<>();

    for (Tuple<Double, Double> p : data) {
      double x = p.getFirst();
      double y = p.getSecond();

      long binIdx = Math.round(x / binSize);
      stats.computeIfAbsent(binIdx, k -> new DoubleSummaryStatistics()).accept(y);
    }

    List<Tuple<Double, Double>> out = new ArrayList<>(stats.size());
    for (var e : stats.entrySet()) {
      long binIdx = e.getKey();
      double b = binIdx * binSize;
      out.add(new Tuple<>(b, e.getValue().getAverage()));
    }
    return out;
  }
}
