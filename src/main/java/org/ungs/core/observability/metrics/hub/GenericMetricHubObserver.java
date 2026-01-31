package org.ungs.core.observability.metrics.hub;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.observability.api.SimulationEvent;
import org.ungs.core.observability.api.SimulationObserver;
import org.ungs.core.observability.metrics.api.ComparisonRenderer;
import org.ungs.core.observability.metrics.api.MetricBundle;
import org.ungs.core.routing.api.AlgorithmType;

@Slf4j
public final class GenericMetricHubObserver implements SimulationObserver {

  private final SimulationConfigContext cfg;
  private final Path outDir;
  private final List<MetricBundle<?>> bundles;

  private final Map<String, Map<AlgorithmType, Object>> snapshotsByMetricId = new LinkedHashMap<>();

  public GenericMetricHubObserver(
      SimulationConfigContext cfg, Path outDir, List<MetricBundle<?>> bundles) {
    this.cfg = cfg;
    this.outDir = outDir;
    this.bundles = List.copyOf(bundles);
  }

  @Override
  public void onAlgorithmStart(SimulationRuntimeContext ctx) {
    for (MetricBundle<?> b : bundles) {
      b.metric().reset();
    }
  }

  @Override
  public void onEvent(SimulationEvent e, SimulationRuntimeContext ctx) {
    for (MetricBundle<?> b : bundles) {
      b.metric().onEvent(e, ctx);
    }
  }

  @Override
  public void onAlgorithmEnd(SimulationRuntimeContext ctx) {
    AlgorithmType algo = ctx.getCurrentAlgorithm();

    for (MetricBundle<?> b0 : bundles) {
      storeSnapshotAndRenderPerAlgorithm(b0, algo);
    }
  }

  @Override
  public void onSimulationEnd(SimulationRuntimeContext ctx) {
    for (MetricBundle<?> b0 : bundles) {
      if (ctx.getConfig().general().algorithms().size() > 1) renderComparisonIfAny(b0);
    }
  }

  private <T> void storeSnapshotAndRenderPerAlgorithm(MetricBundle<T> b, AlgorithmType algo) {
    T snapshot = b.metric().snapshot();

    snapshotsByMetricId
        .computeIfAbsent(b.id(), __ -> new EnumMap<>(AlgorithmType.class))
        .put(algo, snapshot);

    Path metricOut = outDir.resolve(algo.name()).resolve("metrics").resolve(b.id());

    try {
      Files.createDirectories(metricOut);
    } catch (IOException e) {
      throw new RuntimeException("Failed creating output dir: " + metricOut, e);
    }

    try {
      b.perAlgoRenderer().renderPerAlgorithm(metricOut, algo, cfg, snapshot);
    } catch (Exception ex) {
      log.warn("[Metrics] Failed rendering per-algorithm for metric={} algo={}", b.id(), algo, ex);
    }
  }

  private <T> void renderComparisonIfAny(MetricBundle<T> b) {
    ComparisonRenderer<T> comparison = b.comparisonRenderer();
    if (comparison == null) return;

    @SuppressWarnings("unchecked")
    Map<AlgorithmType, T> dataByAlgo =
        (Map<AlgorithmType, T>) snapshotsByMetricId.getOrDefault(b.id(), Map.of());

    if (dataByAlgo.isEmpty()) return;

    Path metricOut = outDir.resolve("comparison").resolve(b.id());
    try {
      Files.createDirectories(metricOut);
    } catch (IOException e) {
      throw new RuntimeException("Failed creating comparison output dir: " + metricOut, e);
    }

    try {
      comparison.renderComparison(metricOut, cfg, dataByAlgo);
    } catch (Exception ex) {
      log.warn("[Metrics] Failed rendering comparison for metric={}", b.id(), ex);
    }
  }
}
