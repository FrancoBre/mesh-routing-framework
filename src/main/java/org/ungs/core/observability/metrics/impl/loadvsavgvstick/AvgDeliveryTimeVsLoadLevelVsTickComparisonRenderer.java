package org.ungs.core.observability.metrics.impl.loadvsavgvstick;

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
import org.ungs.util.Tuple3;

public final class AvgDeliveryTimeVsLoadLevelVsTickComparisonRenderer
    implements ComparisonRenderer<List<Tuple3<Long, Double, Double>>> {

  @Override
  public void renderComparison(
      Path out,
      SimulationConfigContext cfg,
      Map<AlgorithmType, List<Tuple3<Long, Double, Double>>> dataByAlgo) {

    try {
      Files.createDirectories(out);

      XYChart chart =
          new XYChartBuilder()
              .width(1000)
              .height(650)
              .title("Avg Delivery Time & Load Level vs Tick")
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

      styler.setYAxisGroupPosition(1, Styler.YAxisPosition.Right);
      //            styler.setYAxisGroupTitle(1, "Load Level (L)");

      Color[] colors = {
        new Color(40, 90, 160),
        new Color(230, 150, 60),
        new Color(70, 140, 90),
        new Color(150, 80, 160),
        new Color(120, 120, 120)
      };

      BasicStroke dashed =
          new BasicStroke(
              1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {4f, 4f}, 0f);

      Map<AlgorithmType, List<Tuple3<Long, Double, Double>>> ordered =
          (dataByAlgo instanceof LinkedHashMap<?, ?>)
              ? dataByAlgo
              : new LinkedHashMap<>(dataByAlgo);

      int idx = 0;
      for (var entry : ordered.entrySet()) {
        AlgorithmType algo = entry.getKey();
        List<Tuple3<Long, Double, Double>> data = entry.getValue();
        if (data == null || data.isEmpty()) continue;

        double[] x = data.stream().mapToDouble(Tuple3::first).toArray();
        double[] yAvg = data.stream().mapToDouble(Tuple3::second).toArray();
        double[] yL = data.stream().mapToDouble(Tuple3::third).toArray();

        Color c = colors[idx % colors.length];

        XYSeries sAvg = chart.addSeries(algo.name() + " avg", x, yAvg);
        sAvg.setMarker(SeriesMarkers.NONE);
        sAvg.setLineWidth(1.2f);
        sAvg.setLineColor(c);

        XYSeries sL = chart.addSeries(algo.name() + " L", x, yL);
        sL.setMarker(SeriesMarkers.NONE);
        sL.setLineWidth(1.0f);
        sL.setLineColor(c);
        sL.setYAxisGroup(1);
        sL.setLineStyle(dashed);

        idx++;
      }

      Path file = out.resolve("avg_and_load_vs_tick_comparison.png");
      BitmapEncoder.saveBitmap(chart, file.toString(), BitmapEncoder.BitmapFormat.PNG);

    } catch (IOException e) {
      throw new RuntimeException("Failed to render avg & load vs tick comparison", e);
    }
  }
}
