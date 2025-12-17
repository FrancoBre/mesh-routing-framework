package org.ungs.metrics.avgdelivery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChartBuilder;
import org.ungs.core.Packet;
import org.ungs.core.Registry;
import org.ungs.core.Simulation;
import org.ungs.metrics.Metric;
import org.ungs.util.Tuple;

@Slf4j
public class AvgDeliveryTimeMetric implements Metric<List<Tuple<Double, Double>>> {

  private final Registry registry;
  private final int sampleEvery;
  private final List<Tuple<Double, Double>> dataPoints;

  public AvgDeliveryTimeMetric() {
    this.registry = Registry.getInstance();
    this.sampleEvery = 10; // sample every N ticks
    this.dataPoints = new ArrayList<>();
  }

  @Override
  public void collect() {
    if (Simulation.TIME % sampleEvery != 0) {
      return;
    }

    var receivedPackets = registry.getReceivedPackets();
    if (receivedPackets.isEmpty()) {
      return;
    }

    double avgDeliveryTime =
        receivedPackets.stream().mapToDouble(Packet::getArrivalTime).average().orElse(0.0);

    Double currentTime = Simulation.TIME;
    dataPoints.add(new Tuple<>(currentTime, avgDeliveryTime));

    log.debug("[Metric] Time={} AvgDeliveryTime={}", currentTime, avgDeliveryTime);
  }

  @Override
  public List<Tuple<Double, Double>> report() {
    return List.copyOf(dataPoints);
  }

  @Override
  public void reset() {
    dataPoints.clear();
  }

  @Override
  public void plot(String filename) {
    try {
      var chart =
          new XYChartBuilder()
              .width(800)
              .height(600)
              .title("Average Delivery Time vs Simulator Time")
              .xAxisTitle("Simulator Time")
              .yAxisTitle("Average Delivery Time")
              .build();

      double[] xData = dataPoints.stream().mapToDouble(Tuple::getFirst).toArray();
      double[] yData = dataPoints.stream().mapToDouble(Tuple::getSecond).toArray();

      chart.addSeries("Avg Delivery Time", xData, yData);

      BitmapEncoder.saveBitmap(chart, filename, BitmapEncoder.BitmapFormat.PNG);
      log.info("[Metric] Plot saved: {}", filename);
    } catch (IOException e) {
      log.error("Error while plotting metric", e);
    }
  }
}
