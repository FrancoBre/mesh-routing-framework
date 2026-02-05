package org.ungs.core.observability.metrics.impl.loadvsavgvstick;

import java.awt.BasicStroke;
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
import org.ungs.core.observability.metrics.api.MetricRenderer;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.util.Tuple3;

public final class AvgDeliveryTimeVsLoadVsTickLevelRenderer
    implements MetricRenderer<List<Tuple3<Long, Double, Double>>> {

  @Override
  public void renderPerAlgorithm(
      Path out,
      AlgorithmType algo,
      SimulationConfigContext cfg,
      List<Tuple3<Long, Double, Double>> data) {

    try {
      Files.createDirectories(out);

      XYChart chart =
          new XYChartBuilder()
              .width(900)
              .height(600)
              .title(algo.name() + " – Avg Delivery Time & Load Level vs Tick")
              .xAxisTitle("Tick")
              .yAxisTitle("Average Delivery Time (moving avg)")
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

      styler.setYAxisGroupPosition(1, Styler.YAxisPosition.Right);
      //            styler.setYAxisGroupTitle(1, "Load Level (L)");

      if (data == null || data.isEmpty()) {
        Path file = out.resolve("avg_and_load_vs_tick.png");
        BitmapEncoder.saveBitmap(chart, file.toString(), BitmapEncoder.BitmapFormat.PNG);
        return;
      }

      double[] x = data.stream().mapToDouble(Tuple3::first).toArray();
      double[] yAvg = data.stream().mapToDouble(Tuple3::second).toArray();
      double[] yL = data.stream().mapToDouble(Tuple3::third).toArray();

      // Avg series (left axis)
      XYSeries sAvg = chart.addSeries("Avg delivery time", x, yAvg);
      sAvg.setMarker(SeriesMarkers.NONE);
      sAvg.setLineWidth(1.2f);
      sAvg.setLineColor(new Color(40, 90, 160));

      // Load series (right axis)
      XYSeries sL = chart.addSeries("Load level (L)", x, yL);
      sL.setMarker(SeriesMarkers.NONE);
      sL.setLineWidth(1.0f);
      sL.setLineColor(new Color(230, 150, 60));
      sL.setYAxisGroup(1);
      sL.setLineStyle(
          new BasicStroke(
              1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {4f, 4f}, 0f));

      Path file = out.resolve("avg_and_load_vs_tick.png");
      BitmapEncoder.saveBitmap(chart, file.toString(), BitmapEncoder.BitmapFormat.PNG);

    } catch (IOException e) {
      throw new RuntimeException("Failed to render avg & load vs tick", e);
    }
  }
}
