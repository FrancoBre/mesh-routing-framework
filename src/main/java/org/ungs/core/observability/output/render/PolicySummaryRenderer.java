package org.ungs.core.observability.output.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.ungs.core.network.Network;
import org.ungs.core.network.Node;
import org.ungs.core.network.Packet;
import org.ungs.core.observability.events.HopEvent;
import org.ungs.core.observability.events.PacketDeliveredEvent;
import org.ungs.core.routing.api.AlgorithmType;

public final class PolicySummaryRenderer {

  /**
   * Renders a policy summary diagram showing for each node how many of the point-to-point routes
   * pass through that node.
   *
   * @param network the network topology
   * @param hops list of all hop events
   * @param deliveredEvents list of packet delivered events to get origin/destination pairs
   * @param algorithm the algorithm to filter events
   * @param outFile output file path
   */
  public void render(
      Network network,
      List<HopEvent> hops,
      List<PacketDeliveredEvent> deliveredEvents,
      AlgorithmType algorithm,
      Path outFile) {

    // Build a map from packet ID to (origin, destination) pair
    Map<Packet.Id, RoutePair> packetRoutes = new HashMap<>();
    for (PacketDeliveredEvent evt : deliveredEvents) {
      if (evt.algorithm().equals(algorithm)) {
        packetRoutes.put(
            evt.packet().getId(),
            new RoutePair(evt.packet().getOrigin(), evt.packet().getDestination()));
      }
    }

    // For each node, track which (origin, destination) pairs have routes through it
    Map<Node.Id, Set<RoutePair>> routesThroughNode = new HashMap<>();
    for (Node n : network.getNodes()) {
      routesThroughNode.put(n.getId(), new HashSet<>());
    }

    // Process all hops - a route passes through a node if any hop touches that node
    for (HopEvent h : hops) {
      if (!h.algorithm().equals(algorithm)) continue;

      RoutePair pair = packetRoutes.get(h.packetId());
      if (pair == null) continue; // Packet didn't complete, skip

      // Add this route to both the 'from' and 'to' nodes
      routesThroughNode.get(h.from()).add(pair);
      routesThroughNode.get(h.to()).add(pair);
    }

    // Count unique routes per node
    Map<Node.Id, Integer> routeCounts = new HashMap<>();
    for (var entry : routesThroughNode.entrySet()) {
      routeCounts.put(entry.getKey(), entry.getValue().size());
    }

    // Generate the visualization
    Map<Node.Id, Point> pos = grid6x6Positions(network);
    BufferedImage img = drawBase(network, pos, 1100, 900);
    Graphics2D g = img.createGraphics();
    setup(g);

    // Draw nodes with counts
    drawNodesWithCounts(network, pos, routeCounts, g);

    g.dispose();

    try {
      Files.createDirectories(outFile.getParent());
      ImageIO.write(img, "png", outFile.toFile());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Map<Node.Id, Point> grid6x6Positions(Network network) {
    int cell = 130;
    int marginX = 90;
    int marginY = 80;
    int extraGapAfterCol2 = 180;

    Map<Node.Id, Point> pos = new HashMap<>();
    for (Node n : network.getNodes()) {
      int id = n.getId().value();
      int row = id / 6;
      int col = id % 6;

      int x = marginX + col * cell + (col >= 3 ? extraGapAfterCol2 : 0);
      int y = marginY + row * cell;

      pos.put(n.getId(), new Point(x, y));
    }
    return pos;
  }

  private static BufferedImage drawBase(Network network, Map<Node.Id, Point> pos, int w, int h) {
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    setup(g);

    // background
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, w, h);

    // draw all edges light
    g.setStroke(new BasicStroke(2f));
    g.setColor(new Color(120, 150, 255, 130));

    // undirected set so we don't draw twice
    Set<EdgeKey> edges = new HashSet<>();
    for (Node a : network.getNodes()) {
      for (Node b : a.getNeighbors()) {
        edges.add(EdgeKey.undirected(a.getId(), b.getId()));
      }
    }

    for (EdgeKey e : edges) {
      Point pa = pos.get(e.a);
      Point pb = pos.get(e.b);
      if (pa == null || pb == null) continue;
      g.drawLine(pa.x, pa.y, pb.x, pb.y);
    }

    g.dispose();
    return img;
  }

  private static void drawNodesWithCounts(
      Network network, Map<Node.Id, Point> pos, Map<Node.Id, Integer> counts, Graphics2D g) {

    g.setStroke(new BasicStroke(2f));

    for (Node n : network.getNodes()) {
      Point p = pos.get(n.getId());
      if (p == null) continue;

      int count = counts.getOrDefault(n.getId(), 0);

      // Draw node circle
      g.setColor(new Color(200, 40, 40));
      g.fillOval(p.x - 12, p.y - 12, 24, 24);
      g.setColor(Color.BLACK);
      g.drawOval(p.x - 12, p.y - 12, 24, 24);

      // Draw node ID above
      g.setFont(new Font("SansSerif", Font.PLAIN, 14));
      String idStr = String.valueOf(n.getId().value());
      int idWidth = g.getFontMetrics().stringWidth(idStr);
      g.drawString(idStr, p.x - idWidth / 2, p.y - 20);

      // Draw route count below (larger font)
      g.setFont(new Font("SansSerif", Font.BOLD, 18));
      String countStr = String.valueOf(count);
      int countWidth = g.getFontMetrics().stringWidth(countStr);
      g.drawString(countStr, p.x - countWidth / 2, p.y + 32);
    }
  }

  private static void setup(Graphics2D g) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
  }

  private record EdgeKey(Node.Id a, Node.Id b) {
    static EdgeKey undirected(Node.Id x, Node.Id y) {
      return (x.value() <= y.value()) ? new EdgeKey(x, y) : new EdgeKey(y, x);
    }
  }

  private record RoutePair(Node.Id origin, Node.Id destination) {}
}

