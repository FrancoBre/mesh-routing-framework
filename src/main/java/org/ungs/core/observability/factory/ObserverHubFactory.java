package org.ungs.core.observability.factory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NoArgsConstructor;
import org.ungs.core.config.ObservabilityConfig;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.network.Network;
import org.ungs.core.observability.api.CompositeObserverHub;
import org.ungs.core.observability.api.NoOpObserverHub;
import org.ungs.core.observability.api.ObserverHub;
import org.ungs.core.observability.api.SimulationObserver;
import org.ungs.core.observability.metrics.api.MetricBundle;
import org.ungs.core.observability.metrics.api.MetricPreset;
import org.ungs.core.observability.metrics.api.MetricType;
import org.ungs.core.observability.metrics.hub.GenericMetricHubObserver;
import org.ungs.core.observability.metrics.impl.avgdelivery.AvgDeliveryTimePreset;
import org.ungs.core.observability.output.api.OutputBundle;
import org.ungs.core.observability.output.api.OutputPreset;
import org.ungs.core.observability.output.api.OutputType;
import org.ungs.core.observability.output.impl.ConfigDumpOutputPreset;
import org.ungs.core.observability.output.impl.GifRouteOutputPreset;
import org.ungs.core.observability.output.impl.HeatmapOutputPreset;
import org.ungs.core.observability.output.impl.RouteFramesOutputPreset;
import org.ungs.core.observability.route.RouteRecorderObserver;
import org.ungs.util.FileUtils;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class ObserverHubFactory {

  private static final Map<OutputType, OutputPreset> OUTPUT_REGISTRY =
      new EnumMap<>(OutputType.class);
  private static final Map<MetricType, MetricPreset<?>> METRIC_REGISTRY =
      new EnumMap<>(MetricType.class);

  static {
    registerMetric(new AvgDeliveryTimePreset());

    registerOutput(new HeatmapOutputPreset());
    registerOutput(new GifRouteOutputPreset());
    registerOutput(new RouteFramesOutputPreset());
    registerOutput(new ConfigDumpOutputPreset());
  }

  private static void registerOutput(OutputPreset preset) {
    OUTPUT_REGISTRY.put(preset.type(), preset);
  }

  private static void registerMetric(MetricPreset preset) {
    METRIC_REGISTRY.put(preset.type(), preset);
  }

  public static ObserverHub from(SimulationConfigContext simCfg, Network network) {
    ObservabilityConfig cfg = simCfg.observability();

    boolean noOutputs = cfg.outputs() == null || cfg.outputs().isEmpty();
    boolean noMetrics = cfg.metrics() == null || cfg.metrics().isEmpty();
    if (noOutputs && noMetrics) return NoOpObserverHub.INSTANCE;

    List<SimulationObserver> obs = new ArrayList<>();

    RouteRecorderObserver route = new RouteRecorderObserver();
    obs.add(route);

    if (!noMetrics) {
      List<MetricBundle<?>> bundles = new ArrayList<>();

      for (MetricType type : cfg.metrics()) {
        MetricPreset<?> preset = METRIC_REGISTRY.get(type);
        if (preset == null) {
          throw new IllegalArgumentException("Unknown/unregistered metric type: " + type);
        }
        var bundle = preset.createBundle(simCfg, network);
        bundles.add(bundle);

        if (bundle.metric() instanceof SimulationObserver observer) {
          obs.add(observer);
        }
      }

      Path outDir = resolveOutputDir(simCfg);
      obs.add(new GenericMetricHubObserver(simCfg, outDir, bundles));
    }

    if (!noOutputs) {
      Path outDir = resolveOutputDir(simCfg);

      for (OutputType t : cfg.outputs()) {
        OutputPreset p = OUTPUT_REGISTRY.get(t);
        if (p == null) {
          throw new IllegalArgumentException("Unknown/unregistered output type: " + t);
        }

        OutputBundle b = p.createBundle(simCfg, network, route, outDir);
        obs.add(b.observer());
      }
    }

    return new CompositeObserverHub(obs);
  }

  private static Path resolveOutputDir(SimulationConfigContext simCfg) {
    String base =
        Optional.ofNullable(simCfg.general().outputFolder()).orElse(FileUtils.RESULTS_FILE_NAME);
    String name = Optional.ofNullable(simCfg.general().experimentName()).orElse("results");
    return Paths.get(base).resolve(name);
  }
}
