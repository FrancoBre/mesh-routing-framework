package org.ungs.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.ungs.metrics.Metric;
import org.ungs.util.FileUtils;

@Slf4j
@Getter
public class Registry {

  private static final Registry INSTANCE = new Registry();

  private final List<Hop> route;

  private final List<Packet> activePackets;

  private final List<Packet> receivedPackets;

  @Setter private String currentMetricLabel;

  @Setter private List<Metric<?>> metrics;

  private final Map<String, Metric<?>> labeledMetrics;

  private Registry() {
    this.route = new ArrayList<>();
    this.labeledMetrics = new HashMap<>();
    this.activePackets = new ArrayList<>();
    this.receivedPackets = new ArrayList<>();
  }

  private static final String RESULTS_FILE_NAME;

    static {
        try {
            RESULTS_FILE_NAME = FileUtils.getNextResultsFolder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

  public static Registry getInstance() {
    return INSTANCE;
  }

  public void addLabeledMetric(String label, Metric<?> metric) {
    this.labeledMetrics.put(label, metric);
  }

  public boolean allPacketsReceived() {
    return activePackets.isEmpty();
  }

  public void registerHop(Packet packet, Node.Id from, Node.Id to, double sent, double received) {
    Hop hop = new Hop(packet, from, to, sent, received);
    route.add(hop);
  }

  public void registerReceivedPacket(Packet packet) {
    packet.markAsReceived();
    activePackets.remove(packet);
    receivedPackets.add(packet);
  }

  public double getDeliveryTime(Packet packet) {
    List<Hop> hops =
        route.stream()
            .filter(h -> h.packet().equals(packet))
            .sorted(Comparator.comparingDouble(Hop::sent))
            .toList();

    if (hops.isEmpty()) {
      return 0.0;
    }

    double firstSent = hops.get(0).sent();
    double lastReceived = hops.get(hops.size() - 1).received();

    return lastReceived - firstSent;
  }

  public void registerActivePackets(List<Packet> packets) {
    activePackets.addAll(packets);
  }

  public void reset() {
    Simulation.TIME = 0.0;
    route.clear();
    activePackets.clear();
    receivedPackets.clear();
    for (Metric<?> metric : labeledMetrics.values()) {
      metric.reset();
    }
  }

  public void collectMetrics() {
    for (Metric<?> metric : labeledMetrics.values()) {
      metric.collect();
    }
  }

public void plotMetrics(SimulationConfig config) {
      String resultsFileName = RESULTS_FILE_NAME + "/" + currentMetricLabel;

      StringBuilder summary = new StringBuilder();
      summary.append("Simulation Configuration:\n");
      summary.append("Topology: ").append(config.topology()).append("\n");
      summary.append("Algorithms: ").append(config.algorithms()).append("\n");
      summary.append("Total Packets: ").append(config.totalPackets()).append("\n");
      summary.append("Packet Inject Gap: ").append(config.packetInjectGap()).append("\n");
      summary.append("Seed: ").append(config.seed()).append("\n");
      summary.append("Metrics: ").append(config.metrics()).append("\n");
      summary.append("Export To: ").append(config.exportTo()).append("\n");

      try {
          java.nio.file.Files.writeString(
              java.nio.file.Path.of(resultsFileName + "_summary.txt"),
              summary.toString()
          );
      } catch (IOException e) {
          log.warn("Could not write simulation summary: {}", e.getMessage());
      }

      for (Metric<?> metric : labeledMetrics.values()) {
          metric.plot(resultsFileName);
      }
  }

  public record Hop(Packet packet, Node.Id from, Node.Id to, double sent, double received) {}
}
