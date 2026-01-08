package org.ungs.core;

import static org.ungs.util.RouteVisualizer.saveEdgeHeatmapPng;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
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

  @Setter private Network network;

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

  public void plotEverything(SimulationConfig config) {
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
          java.nio.file.Path.of(resultsFileName + "_summary.txt"), summary.toString());
    } catch (IOException e) {
      log.warn("Could not write simulation summary: {}", e.getMessage());
    }

    plotNetworkTopology(RESULTS_FILE_NAME + "/" + "network_topology.png");
    saveEdgeHeatmapPng(this.network, this.route, RESULTS_FILE_NAME + "/" + "route_heatmap.png");

    for (Metric<?> metric : labeledMetrics.values()) {
      metric.plot(resultsFileName);
    }
  }

  public void plotNetworkTopology(String filename) {
    int cols = 6;
    int rows = 6;

    int width = 820;
    int height = 620;

    int marginX = 90;
    int marginY = 70;

    int usableW = width - 2 * marginX;
    int usableH = height - 2 * marginY;

    double stepX = usableW / (double) (cols - 1);
    double stepY = usableH / (double) (rows - 1);

    // styling
    int nodeRadius = 9;
    Stroke edgeStroke = new BasicStroke(2.0f);
    Stroke nodeStroke = new BasicStroke(2.0f);

    Color bg = Color.WHITE;
    Color edgeColor = new Color(60, 90, 220); // blue-ish
    Color nodeFill = new Color(220, 40, 40); // red-ish
    Color nodeBorder = new Color(120, 0, 0);
    Color labelColor = new Color(80, 80, 80);

    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(bg);
      g.fillRect(0, 0, width, height);

      // helper: id -> (x,y)
      record Pt(int x, int y) {}
      Pt[] pos = new Pt[this.network.getNodes().size()];
      for (Node n : this.network.getNodes()) {
        int id = n.getId().value();
        int r = id / cols;
        int c = id % cols;

        int x = (int) Math.round(marginX + c * stepX);
        int y = (int) Math.round(marginY + r * stepY);
        pos[id] = new Pt(x, y);
      }

      // draw edges once (avoid double draw: a<->b)
      g.setStroke(edgeStroke);
      g.setColor(edgeColor);

      Set<Long> drawn = new HashSet<>();
      for (Node a : this.network.getNodes()) {
        int aId = a.getId().value();
        Pt pa = pos[aId];
        if (pa == null) continue;

        for (Node b : a.getNeighbors()) {
          int bId = b.getId().value();
          Pt pb = pos[bId];
          if (pb == null) continue;

          int min = Math.min(aId, bId);
          int max = Math.max(aId, bId);
          long key = (((long) min) << 32) | (max & 0xffffffffL);

          if (drawn.add(key)) {
            g.drawLine(pa.x(), pa.y(), pb.x(), pb.y());
          }
        }
      }

      // draw nodes
      for (Node n : this.network.getNodes()) {
        int id = n.getId().value();
        Pt p = pos[id];
        if (p == null) continue;

        int x = p.x();
        int y = p.y();

        // node circle
        Ellipse2D.Double circle =
            new Ellipse2D.Double(
                x - nodeRadius, y - nodeRadius, nodeRadius * 2.0, nodeRadius * 2.0);

        g.setColor(nodeFill);
        g.fill(circle);

        g.setStroke(nodeStroke);
        g.setColor(nodeBorder);
        g.draw(circle);

        // label (id)
        g.setColor(labelColor);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString(String.valueOf(id), x + nodeRadius + 4, y - nodeRadius - 2);
      }

      // title
      g.setColor(new Color(30, 30, 30));
      g.setFont(new Font("SansSerif", Font.BOLD, 16));
      g.drawString("Network topology (grid 6x6 mapping by node id)", 20, 28);

      // footer
      g.setFont(new Font("SansSerif", Font.PLAIN, 14));
      g.setColor(new Color(50, 50, 50));
      g.drawString("Figure: topology view", 20, height - 18);

      ImageIO.write(img, "png", new File(filename));
      log.info("[Network] Topology plot saved to {}", filename);

    } catch (IOException e) {
      log.error("[Network] Error while saving topology plot", e);
    } finally {
      g.dispose();
    }
  }

  public record Hop(Packet packet, Node.Id from, Node.Id to, double sent, double received) {}
}
