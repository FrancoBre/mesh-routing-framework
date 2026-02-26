package org.ungs.core.observability.metrics.impl.windoweddelivery;

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
import org.ungs.util.Tuple;

public final class WindowedDeliveryTimeRenderer
    implements MetricRenderer<List<Tuple<Long, Double>>> {

  @Override
  public void renderPerAlgorithm(
      Path out, AlgorithmType algo, SimulationConfigContext cfg, List<Tuple<Long, Double>> data) {

    try {
      Files.createDirectories(out);

      XYChart chart =
          new XYChartBuilder()
              .width(800)
              .height(600)
              .title(algo.name() + " – Windowed Avg Delivery Time")
              .xAxisTitle("Tick")
              .yAxisTitle("Windowed Avg Delivery Time")
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
      styler.setMarkerSize(4);

      if (data == null || data.isEmpty()) return;

      double[] x = data.stream().mapToDouble(p -> p.getFirst().doubleValue()).toArray();
      double[] y = data.stream().mapToDouble(Tuple::getSecond).toArray();

      XYSeries series = chart.addSeries(algo.name(), x, y);
      series.setMarker(SeriesMarkers.NONE);
      series.setLineWidth(1.2f);
      series.setLineColor(new Color(40, 90, 160));

      Path file = out.resolve("windowed_delivery_time.png");
      BitmapEncoder.saveBitmap(chart, file.toString(), BitmapEncoder.BitmapFormat.PNG);

    } catch (IOException e) {
      throw new RuntimeException("Failed to render windowed delivery time", e);
    }
  }
}
