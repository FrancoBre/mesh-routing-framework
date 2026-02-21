package org.ungs.core.observability.output.impl;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import org.ungs.core.network.Node;

/**
 * Renders a heatmap of Q-values for a given "from" node.
 *
 * <p>Visualization: - rows: actions (neighbors y of 'from') - cols: destinations d (all nodes, or a
 * subset) - cell value: Q_from(y, d)
 *
 * <p>Notes: - This renderer is intentionally independent of the internal storage of QTable
 * (Set-based, map-based, preallocated, lazy, etc.). It only requires a getter. - Normalizes values
 * per-frame using min/max of the rendered matrix.
 */
public final class QTableHeatmapRenderer {

  public record Config(
      int cellSize,
      int headerHeight,
      int rowLabelWidth,
      int colLabelHeight,
      int margin,
      boolean drawNumbers,
      boolean highlightBestForFixedDestination) {
    public static Config defaults() {
      return new Config(
          26, // cellSize
          60, // headerHeight
          140, // rowLabelWidth
          60, // colLabelHeight
          20, // margin
          false, // drawNumbers (turn on if you want)
          true // highlightBestForFixedDestination
          );
    }
  }

  /** Minimal adapter so you can pass *any* QTable implementation. */
  @FunctionalInterface
  public interface QGetter {
    double get(Node.Id from, Node.Id to, Node.Id destination);
  }

  /**
   * Renders heatmap to outFile.
   *
   * @param tick tick number for label
   * @param algorithmName algo label
   * @param from node whose Q-table we are visualizing
   * @param actionNeighbors neighbors of 'from' (rows)
   * @param destinations destinations to plot (cols) - pass all nodes for full table
   * @param fixedDestination optional: if non-null, we highlight best action for that destination
   * @param q getter for Q(from,to,dest)
   * @param outFile output png path
   */
  public void render(
      long tick,
      String algorithmName,
      Node.Id from,
      List<Node.Id> actionNeighbors,
      List<Node.Id> destinations,
      Node.Id fixedDestination,
      QGetter q,
      Path outFile) {
    Objects.requireNonNull(from);
    Objects.requireNonNull(actionNeighbors);
    Objects.requireNonNull(destinations);
    Objects.requireNonNull(q);
    Objects.requireNonNull(outFile);

    var cfg = Config.defaults();

    // Deterministic order
    actionNeighbors = new ArrayList<>(actionNeighbors);
    actionNeighbors.sort(Comparator.comparingInt(Node.Id::value));
    destinations = new ArrayList<>(destinations);
    destinations.sort(Comparator.comparingInt(Node.Id::value));

    // Build matrix and compute min/max (ignore NaN/inf)
    double[][] m = new double[actionNeighbors.size()][destinations.size()];
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;

    for (int r = 0; r < actionNeighbors.size(); r++) {
      var to = actionNeighbors.get(r);
      for (int c = 0; c < destinations.size(); c++) {
        var d = destinations.get(c);
        double v = q.get(from, to, d);
        m[r][c] = v;

        if (Double.isFinite(v)) {
          if (v < min) {
            min = v;
          }
          if (v > max) {
            max = v;
          }
        }
      }
    }

    if (!Double.isFinite(min) || !Double.isFinite(max) || Math.abs(max - min) < 1e-12) {
      // fallback to avoid div-by-zero
      min = 0.0;
      max = 1.0;
    }

    int rows = actionNeighbors.size();
    int cols = destinations.size();

    int heatW = cols * cfg.cellSize();
    int heatH = rows * cfg.cellSize();

    //    int w = cfg.margin() + cfg.rowLabelWidth() + heatW + cfg.margin();
    //    int h = cfg.margin() + cfg.headerHeight() + cfg.colLabelHeight() + heatH + cfg.margin();
    //
    //              .width(800)
    //          .height(600)

    int w = 500;
    int h = 300;

    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    setup(g);

    // background
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, w, h);

    int originX = cfg.margin() + cfg.rowLabelWidth();
    int originY = cfg.margin() + cfg.headerHeight() + cfg.colLabelHeight();

    // Title/header
    g.setColor(Color.BLACK);
    g.setFont(new Font("SansSerif", Font.BOLD, 18));
    g.drawString("Q-Table heatmap", cfg.margin(), cfg.margin() + 22);

    g.setFont(new Font("SansSerif", Font.PLAIN, 14));
    g.drawString(
        "algo=" + algorithmName + "   tick=" + tick + "   from=" + from.value(),
        cfg.margin(),
        cfg.margin() + 44);

    if (fixedDestination != null) {
      g.drawString("fixedDest=" + fixedDestination.value(), cfg.margin(), cfg.margin() + 62);
    }

    // Column labels (destinations)
    g.setFont(new Font("SansSerif", Font.PLAIN, 12));
    for (int c = 0; c < cols; c++) {
      int x = originX + c * cfg.cellSize();
      int y = cfg.margin() + cfg.headerHeight() + 18;

      String label = String.valueOf(destinations.get(c).value());
      // center-ish
      g.setColor(new Color(0, 0, 0, 180));
      g.drawString(label, x + 6, y);
    }

    // Row labels (neighbors/actions)
    for (int r = 0; r < rows; r++) {
      int x = cfg.margin() + 6;
      int y = originY + r * cfg.cellSize() + (cfg.cellSize() / 2) + 5;

      g.setColor(new Color(0, 0, 0, 200));
      g.drawString("to " + actionNeighbors.get(r).value(), x, y);
    }

    // If we want to highlight best action for a specific destination
    int highlightRow = -1;
    int highlightCol = -1;
    if (cfg.highlightBestForFixedDestination() && fixedDestination != null) {
      int col = indexOf(destinations, fixedDestination);
      if (col >= 0) {
        double best = Double.POSITIVE_INFINITY;
        int bestRow = -1;
        for (int r = 0; r < rows; r++) {
          double v = m[r][col];
          if (v < best) {
            best = v;
            bestRow = r;
          }
        }
        highlightRow = bestRow;
        highlightCol = col;
      }
    }

    // Draw heatmap cells
    DecimalFormat df = new DecimalFormat("0.##");

    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        double v = m[r][c];

        float t = normalize(v, min, max); // 0..1
        Color cellColor = blueHeat(t);

        int x = originX + c * cfg.cellSize();
        int y = originY + r * cfg.cellSize();

        g.setColor(cellColor);
        g.fillRect(x, y, cfg.cellSize(), cfg.cellSize());

        // grid line
        g.setColor(new Color(0, 0, 0, 40));
        g.drawRect(x, y, cfg.cellSize(), cfg.cellSize());

        // optional numbers
        if (cfg.drawNumbers()) {
          g.setFont(new Font("SansSerif", Font.PLAIN, 10));
          g.setColor(new Color(0, 0, 0, 180));
          g.drawString(df.format(v), x + 3, y + cfg.cellSize() - 4);
        }
      }
    }

    // Highlight best cell for fixed dest
    if (highlightRow >= 0 && highlightCol >= 0) {
      int x = originX + highlightCol * cfg.cellSize();
      int y = originY + highlightRow * cfg.cellSize();

      g.setColor(new Color(255, 0, 0, 180));
      g.setStroke(new BasicStroke(3f));
      g.drawRect(x + 1, y + 1, cfg.cellSize() - 2, cfg.cellSize() - 2);

      g.setStroke(new BasicStroke(1f));
    }

    // Legend
    drawLegend(g, originX, cfg.margin() + cfg.headerHeight() + 30, 220, 14, min, max);

    g.dispose();

    try {
      Files.createDirectories(outFile.getParent());
      ImageIO.write(img, "png", outFile.toFile());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  // -------- helpers --------

  private static void setup(Graphics2D g) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
  }

  private static float normalize(double v, double min, double max) {
    if (!Double.isFinite(v)) {
      return 1.0f;
    }
    double t = (v - min) / (max - min);
    if (t < 0) {
      t = 0;
    }
    if (t > 1) {
      t = 1;
    }
    return (float) t;
  }

  /** Simple heat: low=dark, high=light-ish blue. No fancy palettes. */
  private static Color blueHeat(float t) {
    // invert so "lower Q is darker" feels like "better" = more intense
    float inv = 1.0f - t;

    int r = (int) (240 - 180 * inv); // 240..60
    int g = (int) (248 - 190 * inv); // 248..58
    int b = (int) (255 - 60 * inv); // 255..195
    int a = 255;
    return new Color(clamp(r), clamp(g), clamp(b), a);
  }

  private static int clamp(int x) {
    return Math.max(0, Math.min(255, x));
  }

  private static int indexOf(List<Node.Id> list, Node.Id id) {
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).value() == id.value()) {
        return i;
      }
    }
    return -1;
  }

  private static void drawLegend(Graphics2D g, int x, int y, int w, int h, double min, double max) {
    // gradient bar
    for (int i = 0; i < w; i++) {
      float t = i / (float) (w - 1);
      g.setColor(blueHeat(t));
      g.drawLine(x + i, y, x + i, y + h);
    }

    g.setColor(new Color(0, 0, 0, 120));
    g.drawRect(x, y, w, h);

    g.setFont(new Font("SansSerif", Font.PLAIN, 12));
    g.setColor(new Color(0, 0, 0, 200));
    g.drawString(String.format("min=%.2f", min), x, y + h + 16);
    g.drawString(String.format("max=%.2f", max), x + w - 80, y + h + 16);
  }
}
