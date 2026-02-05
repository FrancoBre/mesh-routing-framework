package org.ungs.core.observability.metrics.impl.loadvsavg;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.observability.events.LoadLevelUpdatedEvent;
import org.ungs.core.observability.metrics.api.MetricRenderer;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.util.Tuple;
import org.ungs.util.Tuple3;

public final class AvgDeliveryTimeVsLoadLevelRenderer
    implements MetricRenderer<List<Tuple3<Double, Double, LoadLevelUpdatedEvent.LoadLevelTrend>>> {

  @Override
  public void renderPerAlgorithm(
      Path out,
      AlgorithmType algo,
      SimulationConfigContext cfg,
      List<Tuple3<Double, Double, LoadLevelUpdatedEvent.LoadLevelTrend>> data) {
    try {
      Files.createDirectories(out);

      XYChart chart =
          new XYChartBuilder()
              .width(800)
              .height(600)
              .title(algo.name() + " – Avg Delivery Time vs Load Level")
              .xAxisTitle("Load Level (L)")
              .yAxisTitle("Average Delivery Time")
              .build();

      var styler = chart.getStyler();
      styler.setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
      styler.setChartBackgroundColor(Color.WHITE);
      styler.setPlotBackgroundColor(Color.WHITE);
      styler.setPlotGridLinesVisible(true);
      styler.setPlotGridLinesColor(new Color(220, 220, 220));
      styler.setLegendPosition(Styler.LegendPosition.OutsideE);
      styler.setLegendBorderColor(Color.WHITE);
      styler.setAxisTicksLineVisible(true);
      styler.setAxisTickLabelsColor(Color.DARK_GRAY);
      styler.setAxisTitleFont(styler.getAxisTitleFont().deriveFont(13f));
      styler.setAxisTickLabelsFont(styler.getAxisTickLabelsFont().deriveFont(11f));
      styler.setChartTitleFont(styler.getChartTitleFont().deriveFont(Font.PLAIN, 14f));

      double binSize = 0.02;
      List<Tuple<Double, Double>> binned = binAndAveragePreserveSegments(data, binSize);

      double[] x = binned.stream().mapToDouble(Tuple::getFirst).toArray();
      double[] y = binned.stream().mapToDouble(Tuple::getSecond).toArray();

      XYSeries s = chart.addSeries(algo.name(), x, y);
      s.setMarker(SeriesMarkers.NONE);
      s.setLineWidth(1.2f);
      s.setLineColor(new Color(40, 90, 160));

      Path file = out.resolve("avg_delivery_time_vs_load_level.png");
      BitmapEncoder.saveBitmap(chart, file.toString(), BitmapEncoder.BitmapFormat.PNG);

    } catch (IOException e) {
      throw new RuntimeException("Failed to render avg vs load-level", e);
    }
  }

  /**
   * Binning that preserves temporal order and does NOT globally merge bins that re-appear later
   * (e.g. triangular load schedules: same L appears while rising and again while falling).
   *
   * <p>It groups consecutive points that fall into the same bin, averages Y within that consecutive
   * segment, and outputs one point per segment, keeping the original order.
   */
  private static List<Tuple<Double, Double>> binAndAveragePreserveSegments(
      List<Tuple3<Double, Double, LoadLevelUpdatedEvent.LoadLevelTrend>> data, double binSize) {

    if (binSize <= 0) throw new IllegalArgumentException("binSize must be > 0");
    if (data == null || data.isEmpty()) return List.of();

    List<Tuple<Double, Double>> out = new java.util.ArrayList<>();

    Double currentBin = null;
    double sum = 0.0;
    long count = 0;

    for (Tuple3<Double, Double, LoadLevelUpdatedEvent.LoadLevelTrend> p : data) {
      double x = p.first();
      double y = p.second();

      double b = Math.round(x / binSize) * binSize;

      // Start first bin
      if (currentBin == null) {
        currentBin = b;
      }

      // Bin changed => flush current segment
      if (!bEquals(currentBin, b)) {
        if (count > 0) {
          out.add(new Tuple<>(currentBin, sum / (double) count));
        }
        currentBin = b;
        sum = 0.0;
        count = 0;
      }

      sum += y;
      count++;
    }

    // Flush last segment
    if (currentBin != null && count > 0) {
      out.add(new Tuple<>(currentBin, sum / (double) count));
    }

    return out;
  }

  private static boolean bEquals(double a, double b) {
    return Math.abs(a - b) < 1e-12;
  }
}
