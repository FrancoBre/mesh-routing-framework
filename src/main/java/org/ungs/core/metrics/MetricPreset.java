package org.ungs.core.metrics;

public sealed interface MetricPreset permits AvgDeliveryTimePreset {

  MetricType type();

  Metric createMetric();
}
