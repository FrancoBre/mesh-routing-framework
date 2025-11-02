package org.ungs.metrics;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MetricLoader {

  private static final Map<MetricType, MetricPreset> registry = new EnumMap<>(MetricType.class);

  static {
    register(new AvgDeliveryTimePreset());
  }

  private static void register(MetricPreset preset) {
    registry.put(preset.type(), preset);
  }

  public static List<Metric<?>> createMetrics(List<MetricType> metricTypes) {
    List<Metric<?>> metrics = new ArrayList<>();

    for (MetricType type : metricTypes) {
      MetricPreset preset = registry.get(type);
      if (preset == null) {
        throw new IllegalArgumentException("Unknown metric type: " + type);
      }
      metrics.add(preset.createMetric());
    }

    return metrics;
  }
}
