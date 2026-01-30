package org.ungs.core.observability.metrics.impl.avgdelivery;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.observability.metrics.api.ComparisonRenderer;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.util.Tuple;

public final class AvgDeliveryTimeComparisonRenderer
    implements ComparisonRenderer<List<Tuple<Long, Double>>> {

  @Override
  public void renderComparison(
      Path out,
      SimulationConfigContext cfg,
      Map<AlgorithmType, List<Tuple<Long, Double>>> dataByAlgo) {

    try {
      Files.createDirectories(out);

      XYChart chart =
          new XYChartBuilder()
              .width(900)
              .height(600)
              .title("Average Delivery Time vs Tick")
              .xAxisTitle("Tick")
              .yAxisTitle("Average Delivery Time")
              .build();

      var styler = chart.getStyler();

      styler.setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
      styler.setChartBackgroundColor(Color.WHITE);
      styler.setPlotBackgroundColor(Color.WHITE);

      styler.setPlotGridLinesVisible(true);
      styler.setPlotGridLinesColor(new Color(220, 220, 220)); // <-- ESTE SÍ existe

      styler.setLegendPosition(Styler.LegendPosition.OutsideE);
      styler.setLegendBorderColor(Color.WHITE);

      styler.setAxisTitlesVisible(true);
      styler.setAxisTicksLineVisible(true);
      styler.setAxisTickLabelsColor(Color.DARK_GRAY);
      styler.setAxisTitleFont(styler.getAxisTitleFont().deriveFont(13f));
      styler.setAxisTickLabelsFont(styler.getAxisTickLabelsFont().deriveFont(11f));

      styler.setMarkerSize(4);

      Map<AlgorithmType, List<Tuple<Long, Double>>> ordered =
          (dataByAlgo instanceof LinkedHashMap) ? dataByAlgo : new LinkedHashMap<>(dataByAlgo);

      Color[] colors = {
        new Color(40, 90, 160), // blue
        new Color(230, 150, 60), // orange
        new Color(70, 140, 90), // green
        new Color(150, 80, 160), // purple
        new Color(120, 120, 120) // gray
      };

      BasicStroke[] strokes = {
        new BasicStroke(1.2f),
        new BasicStroke(
            1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {6f, 4f}, 0f),
        new BasicStroke(
            1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {2f, 3f}, 0f),
        new BasicStroke(
            1.2f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
            1.0f,
            new float[] {10f, 4f, 2f, 4f},
            0f)
      };

      int idx = 0;

      for (var entry : ordered.entrySet()) {
        AlgorithmType algo = entry.getKey();
        var points = entry.getValue();
        if (points == null || points.isEmpty()) continue;

        double[] x = points.stream().mapToDouble(p -> ((Number) p.getFirst()).doubleValue()).toArray();
        double[] y = points.stream().mapToDouble(Tuple::getSecond).toArray();

        XYSeries series = chart.addSeries(algo.name(), x, y);

        series.setMarker(SeriesMarkers.NONE);
        series.setLineWidth(1.2f);

        // cycle style
        series.setLineColor(colors[idx % colors.length]);
        series.setLineStyle(strokes[idx % strokes.length]);

        idx++;
      }

      Path file = out.resolve("avg_delivery_time_comparison.png");
      BitmapEncoder.saveBitmap(chart, file.toString(), BitmapEncoder.BitmapFormat.PNG);

    } catch (IOException e) {
      throw new RuntimeException("Failed to render comparison", e);
    }
  }
}
