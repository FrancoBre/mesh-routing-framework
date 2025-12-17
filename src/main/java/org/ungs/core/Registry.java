package org.ungs.core;

import java.io.IOException;
import java.util.ArrayList;
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

  public void registerActivePackets(List<Packet> packets) {
    activePackets.addAll(packets);
  }

  public void reset() {
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

  public void plotMetrics() {
    try {
      String resultsFileName = FileUtils.getNextResultsFolder() + "/" + currentMetricLabel;

      for (Metric<?> metric : labeledMetrics.values()) {
        metric.plot(resultsFileName);
      }
    } catch (IOException e) {
      log.error("Failed to create results folder", e);
    }
  }

  public record Hop(Packet packet, Node.Id from, Node.Id to, double sent, double received) {}
}
