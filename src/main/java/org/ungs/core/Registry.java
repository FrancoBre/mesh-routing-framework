package org.ungs.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.ungs.metrics.Metric;

public class Registry {

  private static final Registry INSTANCE = new Registry();

  @Getter private List<Hop> route;

  @Setter private List<Packet> packetsToBeReceived;

  @Getter private List<Packet> receivedPackets;

  @Setter private String currentMetricLabel;

  @Getter @Setter private List<Metric<?>> metrics;

  private Map<String, Metric<?>> labeledMetrics;

  private Registry() {
    this.route = new ArrayList<>();
    this.receivedPackets = new ArrayList<>();
    this.labeledMetrics = new HashMap<>();
  }

  public static Registry getInstance() {
    return INSTANCE;
  }

  public void addLabeledMetric(String label, Metric<?> metric) {
    this.labeledMetrics.put(label, metric);
  }

  public boolean allPacketsReceived() {
    return packetsToBeReceived != null
        && packetsToBeReceived.stream().allMatch(Packet::isReachedDestination);
  }

  public void registerHop(Packet packet, Node.Id from, Node.Id to, double sent, double received) {
    Hop hop = new Hop(packet, from, to, sent, received);
    route.add(hop);
  }

  public void registerReceivedPacket(Packet packet) {
    receivedPackets.add(packet);
  }

  public void collectMetrics() {
    for (Metric<?> metric : labeledMetrics.values()) {
      metric.collect();
    }
  }

  public record Hop(Packet packet, Node.Id from, Node.Id to, double sent, double received) {}
}
