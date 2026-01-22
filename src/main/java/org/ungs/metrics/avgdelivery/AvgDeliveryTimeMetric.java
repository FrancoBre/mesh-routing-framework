package org.ungs.metrics.avgdelivery;

import java.awt.BasicStroke;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchart.AnnotationTextPanel;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.Marker;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.ungs.core.Packet;
import org.ungs.core.Registry;
import org.ungs.core.Simulation;
import org.ungs.core.SimulationConfig;
import org.ungs.metrics.Metric;
import org.ungs.routing.AlgorithmType;
import org.ungs.util.Tuple;

@Slf4j
public class AvgDeliveryTimeMetric implements Metric<List<Tuple<Double, Double>>> {

  private final Registry registry;
  private final int sampleEvery;
  @Getter private final List<Tuple<Double, Double>> dataPoints;

  @Getter private final Map<AlgorithmType, List<Tuple<Double, Double>>> dataPointsPerAlgorithm;

  public AvgDeliveryTimeMetric() {
    this.registry = Registry.getInstance();
    this.sampleEvery = 10; // sample every N ticks
    this.dataPoints = new ArrayList<>();
    this.dataPointsPerAlgorithm = new HashMap<>();
  }

  @Override
  public void collect() {
    if (Simulation.TIME < 6000 || Simulation.TIME % sampleEvery != 0) return;

    var receivedPackets = registry.getReceivedPackets();
    if (receivedPackets.isEmpty()) {
      return;
    }

    double avgDeliveryTime =
        receivedPackets.stream().mapToDouble(Packet::getDeliveryTime).average().orElse(0.0);

    //    boolean measuring = QuiescenceDetector.update(Simulation.TIME, avgDeliveryTime);
    //
    //    if (!measuring) {
    //      return; // todavía en warmup / aprendizaje
    //    }

    Double currentTime = Simulation.TIME;
    dataPoints.add(new Tuple<>(currentTime, avgDeliveryTime));

    log.debug("[Metric] Time={} AvgDeliveryTime={}", currentTime, avgDeliveryTime);
  }

  @Override
  public List<Tuple<Double, Double>> report() {
    return List.copyOf(dataPoints);
  }

  @Override
  public void reset() {
    dataPointsPerAlgorithm.put(registry.getCurrentAlgorithm(), report());
    dataPoints.clear();
  }

  @Override
  public void plot(String filename, AlgorithmType algorithmType, SimulationConfig config) {
    try {
      XYChart chart =
          new XYChartBuilder()
              .width(800)
              .height(600)
              .title(
                  String.format("%s Average Delivery Time vs Simulator Time", algorithmType.name()))
              .xAxisTitle("Simulator Time")
              .yAxisTitle("Average Delivery Time")
              .build();

      double[] xData = dataPoints.stream().mapToDouble(Tuple::getFirst).toArray();
      double[] yData = dataPoints.stream().mapToDouble(Tuple::getSecond).toArray();

      chart.addSeries("Avg Delivery Time", xData, yData);

      double paddingX = chart.getWidth() - 20;
      double paddingY = 20;

      AnnotationTextPanel infoPanel =
          new AnnotationTextPanel(config.toString(), paddingX, paddingY, true);
      //      chart.addAnnotation(infoPanel);

      BitmapEncoder.saveBitmap(chart, filename, BitmapEncoder.BitmapFormat.PNG);
      log.info("[Metric] Plot saved: {}", filename);

    } catch (IOException e) {
      log.error("Error while plotting metric", e);
    }
  }

  @Override
  public void plotAll(String filename, SimulationConfig config) {

    try {
      XYChart chart =
          new XYChartBuilder()
              .width(900)
              .height(600)
              .title("Average Delivery Time vs Simulator Time")
              .xAxisTitle("Simulator Time")
              .yAxisTitle("Average Delivery Time")
              .build();

      chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
      chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
      chart.getStyler().setMarkerSize(6);

      // === styles cíclicos ===
      BasicStroke[] lineStyles = {
        SeriesLines.SOLID, SeriesLines.DASH_DASH, SeriesLines.DASH_DOT, SeriesLines.DOT_DOT
      };

      Marker[] markerStyles = {
        SeriesMarkers.CIRCLE, SeriesMarkers.NONE, SeriesMarkers.DIAMOND, SeriesMarkers.SQUARE
      };

      int idx = 0;

      for (var entry : dataPointsPerAlgorithm.entrySet()) {
        AlgorithmType algo = entry.getKey();
        List<Tuple<Double, Double>> points = entry.getValue();

        double[] x = points.stream().mapToDouble(Tuple::getFirst).toArray();
        double[] y = points.stream().mapToDouble(Tuple::getSecond).toArray();

        XYSeries series = chart.addSeries(algo.name(), x, y);

        // cycled styles
        series.setLineStyle(lineStyles[idx % lineStyles.length]);
        series.setMarker(markerStyles[idx % markerStyles.length]);

        idx++;
      }

      // panel con la config
      double paddingX = chart.getWidth() - 20;
      double paddingY = 20;

      AnnotationTextPanel infoPanel =
          new AnnotationTextPanel(config.toString(), paddingX, paddingY, true);
      chart.addAnnotation(infoPanel);

      // guardar
      BitmapEncoder.saveBitmap(chart, filename, BitmapEncoder.BitmapFormat.PNG);
      log.info("[Metric] Multi-comparison plot saved: {}", filename);

    } catch (IOException e) {
      log.error("Error while plotting multi comparison metric", e);
    }
  }

  //    Map<AlgorithmType, List<Tuple<Double, Double>>> dataPointsPerAlgorithm

  public static void plotAll(
      String filename,
      SimulationConfig config,
      Map<AlgorithmType, List<Tuple<Double, Double>>> dataPointsPerAlgorithm) {

    try {
      XYChart chart =
          new XYChartBuilder()
              .width(900)
              .height(600)
              .title("Average Delivery Time vs Simulator Time")
              .xAxisTitle("Simulator Time")
              .yAxisTitle("Average Delivery Time")
              .build();

      chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
      chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
      chart.getStyler().setMarkerSize(6);

      // === styles cíclicos ===
      BasicStroke[] lineStyles = {
        SeriesLines.SOLID, SeriesLines.DASH_DASH, SeriesLines.DASH_DOT, SeriesLines.DOT_DOT
      };

      Marker[] markerStyles = {
        SeriesMarkers.CIRCLE, SeriesMarkers.NONE, SeriesMarkers.DIAMOND, SeriesMarkers.SQUARE
      };

      int idx = 0;

      for (var entry : dataPointsPerAlgorithm.entrySet()) {
        AlgorithmType algo = entry.getKey();
        List<Tuple<Double, Double>> points = entry.getValue();

        double[] x = points.stream().mapToDouble(Tuple::getFirst).toArray();
        double[] y = points.stream().mapToDouble(Tuple::getSecond).toArray();

        XYSeries series = chart.addSeries(algo.name(), x, y);

        // cycled styles
        series.setLineStyle(lineStyles[idx % lineStyles.length]);
        series.setMarker(markerStyles[idx % markerStyles.length]);

        idx++;
      }

      // panel con la config
      double paddingX = chart.getWidth() - 20;
      double paddingY = 20;

      AnnotationTextPanel infoPanel =
          new AnnotationTextPanel(config.toString(), paddingX, paddingY, true);
      chart.addAnnotation(infoPanel);

      // guardar
      BitmapEncoder.saveBitmap(chart, filename, BitmapEncoder.BitmapFormat.PNG);
      log.info("[Metric] Multi-comparison plot saved: {}", filename);

    } catch (IOException e) {
      log.error("Error while plotting multi comparison metric", e);
    }
  }
}
