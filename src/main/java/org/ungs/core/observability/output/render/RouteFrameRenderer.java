package org.ungs.core.observability.output.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
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

public final class RouteFrameRenderer {

  public void renderTickFrame(
      Network network, long tick, List<HopEvent> hopsThisTick, Path outFile) {
    Map<Node.Id, Point> pos = grid6x6Positions(network);

    BufferedImage img = drawBase(network, pos, 1100, 900);
    Graphics2D g = img.createGraphics();
    setup(g);

    // draw hops
    for (HopEvent h : hopsThisTick) {
      Point a = pos.get(h.from());
      Point b = pos.get(h.to());
      if (a == null || b == null) continue;

      g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g.setColor(colorForPacket(h.packetId()));
      g.drawLine(a.x, a.y, b.x, b.y);
      drawArrowHead(g, a, b);
    }

    drawNodes(network, pos, g);

    // queue sizes
    g.setFont(new Font("SansSerif", Font.BOLD, 16));
    for (Node node : network.getNodes()) {
      Point p = pos.get(node.getId());
      if (p == null) continue;

      int qSize = node.getQueue().size();
      int bx = p.x + 14;
      int by = p.y - 42;

      g.setColor(new Color(0, 0, 0, 160));
      g.fillRoundRect(bx - 2, by - 16, 32, 22, 8, 8);

      g.setColor(Color.WHITE);
      g.drawString(String.valueOf(qSize), bx + 6, by);
    }

    // tick label
    g.setColor(Color.BLACK);
    g.setFont(new Font("SansSerif", Font.BOLD, 22));
    g.drawString("tick = " + tick + "  sends=" + hopsThisTick.size(), 30, 40);

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

  private static Color colorForPacket(Packet.Id packetId) {
    int id = packetId.value();

    // hue ∈ [0,1), bien distribuido
    float hue = (id * 0.61803398875f) % 1.0f; // golden ratio trick
    float saturation = 0.85f;
    float brightness = 0.95f;

    Color base = Color.getHSBColor(hue, saturation, brightness);

    // alpha para ver superposición
    return new Color(base.getRed(), base.getGreen(), base.getBlue(), 200);
  }
}
