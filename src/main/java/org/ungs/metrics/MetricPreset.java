package org.ungs.metrics;

public sealed interface MetricPreset permits AvgDeliveryTimePreset {

  MetricType type();

  Metric createMetric();
}
