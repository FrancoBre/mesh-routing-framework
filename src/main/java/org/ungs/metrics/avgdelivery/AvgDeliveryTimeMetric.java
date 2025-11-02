package org.ungs.metrics.avgdelivery;

import java.util.ArrayList;
import java.util.List;
import org.ungs.core.Registry;
import org.ungs.core.Simulation;
import org.ungs.metrics.Metric;
import org.ungs.util.Tuple;

// list of (totalPacketsSoFar, avgDeliveryTimeSoFar) tuples
public class AvgDeliveryTimeMetric implements Metric<List<Tuple<Integer, Double>>> {

  private final Registry registry;

  private final int sampleEvery;

  private final List<Tuple<Integer, Double>> dataPoints;

  public AvgDeliveryTimeMetric() {
    this.registry = Registry.getInstance();
    this.sampleEvery = 10;
    this.dataPoints = new ArrayList<>();
  }

  @Override
  public void collect() {
    if (Simulation.TIME % sampleEvery == 0) {
      int routeSize = registry.getRoute().size();
      double avgReceivedPackets = registry.getReceivedPackets().size() / (double) routeSize;
      dataPoints.add(new Tuple<>(routeSize, avgReceivedPackets));
    }
  }

  @Override
  public List<Tuple<Integer, Double>> report() {
    return dataPoints;
  }

  @Override
  public void reset() {
    dataPoints.clear();
  }

  @Override
  public void plot(String filename) {
    // TODO  plot avgDeliveryTime vs totalPackets
  }
}
