package org.ungs.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.ungs.core.Network;
import org.ungs.core.Node;
import org.ungs.core.Registry;

public final class RouteVisualizer {

  // ---------- public API ----------
  public static void saveEdgeHeatmapPng(Network network, List<Registry.Hop> route, String outFile) {
    Map<Node.Id, Point> pos = grid6x6Positions(network);

    // undirected edge key: (min,max)
    Map<EdgeKey, Integer> edgeCounts = new HashMap<>();
    for (Registry.Hop h : route) {
      EdgeKey k = EdgeKey.undirected(h.from(), h.to());
      edgeCounts.merge(k, 1, Integer::sum);
    }

    int max = edgeCounts.values().stream().max(Integer::compareTo).orElse(1);

    BufferedImage img = drawBase(network, pos, 1100, 900);
    Graphics2D g = img.createGraphics();
    setup(g);

    // draw edges with thickness proportional to usage
    for (var e : edgeCounts.entrySet()) {
      EdgeKey k = e.getKey();
      int c = e.getValue();
      float w = 1.0f + 10.0f * (c / (float) max); // 1..11 px

      Point a = pos.get(k.a);
      Point b = pos.get(k.b);
      if (a == null || b == null) continue;

      g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.setColor(new Color(50, 90, 200, 160)); // blue-ish
      g.drawLine(a.x, a.y, b.x, b.y);
    }

    // redraw nodes on top
    drawNodes(network, pos, g);

    g.dispose();
    write(img, outFile);
  }

  public static void saveTickFramesPng(
      Network network, List<Registry.Hop> route, int tFrom, int tTo, String dir) {
    Map<Node.Id, Point> pos = grid6x6Positions(network);

    // group hops by sent time (cast to int ticks)
    Map<Integer, List<Registry.Hop>> byTick =
        route.stream().collect(Collectors.groupingBy(h -> (int) Math.floor(h.sent())));

    new File(dir).mkdirs();

    for (int t = tFrom; t <= tTo; t++) {
      BufferedImage img = drawBase(network, pos, 1100, 900);
      Graphics2D g = img.createGraphics();
      setup(g);

      // highlight hops sent at tick t
      List<Registry.Hop> hops = byTick.getOrDefault(t, List.of());
      for (Registry.Hop h : hops) {
        Point a = pos.get(h.from());
        Point b = pos.get(h.to());
        if (a == null || b == null) continue;

        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(220, 50, 50, 200)); // red-ish
        g.drawLine(a.x, a.y, b.x, b.y);

        // tiny arrow head
        drawArrowHead(g, a, b);
      }

      drawNodes(network, pos, g);

      // tick label
      g.setColor(Color.BLACK);
      g.setFont(new Font("SansSerif", Font.BOLD, 22));
      g.drawString("tick = " + t + "  hops=" + hops.size(), 30, 40);

      g.dispose();
      write(img, dir + "/frame_" + String.format("%05d", t) + ".png");
    }
  }

  // ---------- layout ----------
  private static Map<Node.Id, Point> grid6x6Positions(Network network) {
    // positions based on id: row=id/6, col=id%6
    // also add a horizontal gap between col 2 and col 3 to reflect the "split" visually (like your
    // image)
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

    drawNodes(network, pos, g);

    g.dispose();
    return img;
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

  private static void drawArrowHead(Graphics2D g, Point a, Point b) {
    double dx = b.x - a.x;
    double dy = b.y - a.y;
    double len = Math.sqrt(dx * dx + dy * dy);
    if (len < 1e-6) return;

    double ux = dx / len;
    double uy = dy / len;

    int arrowSize = 10;
    int ax = (int) (b.x - ux * 18);
    int ay = (int) (b.y - uy * 18);

    int leftX = (int) (ax - uy * arrowSize);
    int leftY = (int) (ay + ux * arrowSize);

    int rightX = (int) (ax + uy * arrowSize);
    int rightY = (int) (ay - ux * arrowSize);

    Polygon tri = new Polygon(new int[] {b.x, leftX, rightX}, new int[] {b.y, leftY, rightY}, 3);
    g.fillPolygon(tri);
  }

  private static void setup(Graphics2D g) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
  }

  private static void write(BufferedImage img, String outFile) {
    try {
      ImageIO.write(img, "png", new File(outFile));
    } catch (Exception e) {
      throw new RuntimeException("Failed to write image: " + outFile, e);
    }
  }

  private record EdgeKey(Node.Id a, Node.Id b) {
    static EdgeKey undirected(Node.Id x, Node.Id y) {
      return (x.value() <= y.value()) ? new EdgeKey(x, y) : new EdgeKey(y, x);
    }
  }
}
