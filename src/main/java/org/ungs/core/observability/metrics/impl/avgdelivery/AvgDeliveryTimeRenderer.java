package org.ungs.core.observability.metrics.impl.avgdelivery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.observability.metrics.api.MetricRenderer;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.util.Tuple;

public final class AvgDeliveryTimeRenderer implements MetricRenderer<List<Tuple<Long, Double>>> {

  @Override
  public void renderPerAlgorithm(
      Path out, AlgorithmType algo, SimulationConfigContext cfg, List<Tuple<Long, Double>> data) {
    try {
      Files.createDirectories(out);

      XYChart chart =
          new XYChartBuilder()
              .width(800)
              .height(600)
              .title(algo.name() + " Average Delivery Time vs Tick")
              .xAxisTitle("Tick")
              .yAxisTitle("Average Delivery Time")
              .build();

      double[] x = data.stream().mapToDouble(p -> p.getFirst().doubleValue()).toArray();
      double[] y = data.stream().mapToDouble(Tuple::getSecond).toArray();

      chart.addSeries("Avg Delivery Time", x, y);

      Path file = out.resolve("avg_delivery_time.png");
      BitmapEncoder.saveBitmap(chart, file.toString(), BitmapEncoder.BitmapFormat.PNG);

    } catch (IOException e) {
      throw new RuntimeException("Failed to render avg delivery time", e);
    }
  }
}
