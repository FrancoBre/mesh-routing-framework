package org.ungs.core;

import static org.ungs.util.RouteVisualizer.saveEdgeHeatmapPng;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.ungs.metrics.avgdelivery.AvgDeliveryTimeMetric;
import org.ungs.routing.AlgorithmType;
import org.ungs.util.FileUtils;
import org.ungs.util.Tuple;

@Slf4j
@Getter
public class Registry {

  private static final Registry INSTANCE = new Registry();

  private final List<Hop> route;

  private final List<Packet> activePackets;

  private final List<Packet> receivedPackets;

  @Setter private String currentMetricLabel;

  @Getter @Setter private AlgorithmType currentAlgorithm;

  @Setter private List<Metric<?>> metrics;

  @Setter private Network network;

  private final Map<String, Metric<?>> labeledMetrics;

  Map<AlgorithmType, AvgDeliveryTimeMetric> metricsByAlgorithm;

  private Registry() {
    this.route = new ArrayList<>();
    this.labeledMetrics = new HashMap<>();
    this.activePackets = new ArrayList<>();
    this.receivedPackets = new ArrayList<>();
    this.metricsByAlgorithm = new HashMap<>();
    this.currentAlgorithm = null;
  }

  public static final String RESULTS_FILE_NAME;

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
    Hop hop = new Hop(packet, from, to, sent, received, currentAlgorithm);
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
    currentAlgorithm = null;
  }

  public void resetAll() {
    route.clear();
    activePackets.clear();
    receivedPackets.clear();
    for (Metric<?> metric : labeledMetrics.values()) {
      metric.reset();
    }
    currentAlgorithm = null;
  }

  public void collectMetrics() {
    // warmup
    if (Simulation.TIME < 1000) {
      return;
    }
    for (Metric<?> metric : labeledMetrics.values()) {
      metric.collect();
    }
  }

  public void plotAlgorithmSpecific(SimulationConfig config) throws IOException {

    // 0) configuration.txt en el root (solo una vez)
    writeConfiguration(config);

    Path root = rootDir();
    Files.createDirectories(root);

    Path algoDir = algorithmDir(currentAlgorithm);
    Path framesDir = algorithmFramesDir(currentAlgorithm);
    Files.createDirectories(algoDir);
    Files.createDirectories(framesDir);

    Path topologyPng = networkTopologyFile();
    if (!Files.exists(topologyPng)) {
      plotNetworkTopology(topologyPng.toString());
    }

    Path heatmapPng = algoDir.resolve("route_heatmap.png");
    saveEdgeHeatmapPng(this.network, this.route);
    log.info("[Registry] Heatmap saved to {}", heatmapPng);

//    Path gifPath = algorithmRouteGif(currentAlgorithm);
//    createGifFromPngFolder(framesDir.toString(), gifPath.toString(), 1);
//    log.info("[Registry] Route GIF saved to {}", gifPath);

    for (Map.Entry<String, Metric<?>> entry : labeledMetrics.entrySet()) {
      String label = entry.getKey();
      Metric<?> metric = entry.getValue();

      if (!label.startsWith(currentAlgorithm.name() + "-")) {
        continue;
      }

      Path basePath = algoDir.resolve(label);
      metric.plot(basePath.toString(), currentAlgorithm, config);
      log.info("[Registry] Metric plot saved to {}.png", basePath);
    }
  }

  public void plotEverything(SimulationConfig config) {

    Map<AlgorithmType, List<Tuple<Double, Double>>> allDataPoints = new HashMap<>();
    for (Map.Entry<String, Metric<?>> entry : labeledMetrics.entrySet()) {
      String label = entry.getKey();
      Metric<?> metric = entry.getValue();

      if (!label.startsWith(currentAlgorithm.name() + "-")) {
        continue;
      }

      allDataPoints.put(currentAlgorithm, ((AvgDeliveryTimeMetric) metric).getDataPoints());
    }
    AvgDeliveryTimeMetric.plotAll(
        RESULTS_FILE_NAME + "/" + "comparison.png", config, allDataPoints);
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

  public static void createGifFromPngFolder(String folderPath, String outputGifPath, int delayMs)
      throws IOException {
    File dir = new File(folderPath);
    File[] pngFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
    if (pngFiles == null || pngFiles.length == 0) {
      throw new IllegalArgumentException("No PNG files found in folder: " + folderPath);
    }
    Arrays.sort(pngFiles); // Sort by filename

    try (FileOutputStream output = new FileOutputStream(outputGifPath)) {
      AnimatedGifEncoder encoder = new AnimatedGifEncoder();
      encoder.start(output);
      encoder.setDelay(delayMs); // delay in ms
      encoder.setRepeat(0); // 0 = infinite loop

      for (File png : pngFiles) {
        BufferedImage img = ImageIO.read(png);
        encoder.addFrame(img);
      }
      encoder.finish();
    }
  }

  public void writeConfiguration(SimulationConfig config) throws IOException {
    Path configFile = configurationFile();

    if (Files.exists(configFile)) {
      return;
    }

    Files.createDirectories(configFile.getParent());
    Files.writeString(configFile, config.toString());
  }

  public static Path rootDir() {
    return Paths.get(RESULTS_FILE_NAME);
  }

  public static Path algorithmDir(AlgorithmType algorithm) {
    return rootDir().resolve(algorithm.name());
  }

  public static Path algorithmFramesDir(AlgorithmType algorithm) {
    return algorithmDir(algorithm).resolve("frames");
  }

  public static Path algorithmRouteGif(AlgorithmType algorithm) {
    return algorithmDir(algorithm).resolve("route.gif");
  }

  public static Path configurationFile() {
    return rootDir().resolve("configuration.txt");
  }

  public static Path networkTopologyFile() {
    return rootDir().resolve("network_topology.png");
  }

  public record Hop(
      Packet packet,
      Node.Id from,
      Node.Id to,
      double sent,
      double received,
      AlgorithmType algorithm) {}
}
