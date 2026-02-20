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
import java.util.OptionalLong;
import java.util.Set;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.ungs.core.network.Network;
import org.ungs.core.network.Node;
import org.ungs.core.observability.events.HopEvent;
import org.ungs.core.routing.api.AlgorithmType;

@Slf4j
public final class RouteHeatmapRenderer {

  public void render(
      Network network,
      List<HopEvent> hops,
      AlgorithmType algorithm,
      Path outFile,
      long fromTick,
      OptionalLong toTick) {
    Map<Node.Id, Point> pos = grid6x6Positions(network);

    long upperBound = toTick.orElse(Long.MAX_VALUE);

    Map<EdgeKey, Integer> edgeCounts = new HashMap<>();
    for (HopEvent h : hops) {
      if (h.algorithm().equals(algorithm)) {
        // Filter by tick range: sentTick >= fromTick && sentTick < toTick
        long hopTick = h.sentTick();
        if (hopTick >= fromTick && hopTick < upperBound) {
          EdgeKey k = EdgeKey.undirected(h.from(), h.to());
          edgeCounts.merge(k, 1, Integer::sum);
        }
      }
    }

    int max = edgeCounts.values().stream().max(Integer::compareTo).orElse(1);

    BufferedImage img = drawBase(network, pos);
    Graphics2D g = img.createGraphics();
    setup(g);

    for (var e : edgeCounts.entrySet()) {
      EdgeKey k = e.getKey();
      int c = e.getValue();
      float w = 1.0f + 10.0f * (c / (float) max);

      Point a = pos.get(k.a);
      Point b = pos.get(k.b);
      if (a == null || b == null) continue;

      g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.setColor(new Color(50, 90, 200, 160));
      g.drawLine(a.x, a.y, b.x, b.y);
    }

    // Draw edge count labels on top of edges
    drawEdgeLabels(g, pos, edgeCounts);

    drawNodes(network, pos, g);
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

  // ---------- drawing primitives ----------
  private static BufferedImage drawBase(Network network, Map<Node.Id, Point> pos) {
    BufferedImage img = new BufferedImage(1100, 900, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    setup(g);

    // background
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, 1100, 900);

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

    drawNodes(network, pos, g);

    g.dispose();
    return img;
  }

  private static void drawEdgeLabels(
      Graphics2D g, Map<Node.Id, Point> pos, Map<EdgeKey, Integer> edgeCounts) {
    g.setFont(new Font("SansSerif", Font.BOLD, 11));
    var fm = g.getFontMetrics();

    for (var e : edgeCounts.entrySet()) {
      EdgeKey k = e.getKey();
      int count = e.getValue();
      if (count == 0) continue;

      Point a = pos.get(k.a);
      Point b = pos.get(k.b);
      if (a == null || b == null) continue;

      // Calculate midpoint of edge
      int midX = (a.x + b.x) / 2;
      int midY = (a.y + b.y) / 2;

      // Offset slightly perpendicular to the edge to avoid overlap with the line
      double dx = b.x - a.x;
      double dy = b.y - a.y;
      double len = Math.sqrt(dx * dx + dy * dy);
      if (len > 0) {
        // Perpendicular offset (10 pixels)
        int offsetX = (int) (-dy / len * 12);
        int offsetY = (int) (dx / len * 12);
        midX += offsetX;
        midY += offsetY;
      }

      String label = String.valueOf(count);
      int textWidth = fm.stringWidth(label);
      int textHeight = fm.getHeight();

      // Draw white background rectangle for readability
      g.setColor(new Color(255, 255, 255, 220));
      g.fillRoundRect(
          midX - textWidth / 2 - 3, midY - textHeight / 2 - 1, textWidth + 6, textHeight + 2, 4, 4);

      // Draw black border
      g.setColor(new Color(100, 100, 100));
      g.drawRoundRect(
          midX - textWidth / 2 - 3, midY - textHeight / 2 - 1, textWidth + 6, textHeight + 2, 4, 4);

      // Draw the count text
      g.setColor(new Color(30, 30, 30));
      g.drawString(label, midX - textWidth / 2, midY + textHeight / 4);
    }
  }

  private static void drawNodes(Network network, Map<Node.Id, Point> pos, Graphics2D g) {
    g.setStroke(new BasicStroke(2f));
    for (Node n : network.getNodes()) {
      Point p = pos.get(n.getId());
      if (p == null) continue;

      // node
      g.setColor(new Color(200, 40, 40));
      g.fillOval(p.x - 12, p.y - 12, 24, 24);
      g.setColor(Color.BLACK);
      g.drawOval(p.x - 12, p.y - 12, 24, 24);

      // id label
      g.setFont(new Font("SansSerif", Font.PLAIN, 16));
      g.drawString(String.valueOf(n.getId().value()), p.x - 6, p.y - 18);
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
}
