package org.ungs.core.observability.metrics.impl.avgdelivery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
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

      for (var entry : dataByAlgo.entrySet()) {
        AlgorithmType algo = entry.getKey();
        var points = entry.getValue();

        double[] x =
            points.stream().mapToDouble(p -> ((Number) p.getFirst()).doubleValue()).toArray();
        double[] y = points.stream().mapToDouble(Tuple::getSecond).toArray();

        chart.addSeries(algo.name(), x, y);
      }

      Path file = out.resolve("avg_delivery_time_comparison.png");
      BitmapEncoder.saveBitmap(chart, file.toString(), BitmapEncoder.BitmapFormat.PNG);

    } catch (IOException e) {
      throw new RuntimeException("Failed to render comparison", e);
    }
  }
}
