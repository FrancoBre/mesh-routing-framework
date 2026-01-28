package org.ungs.core.observability.metrics.api;

public record MetricBundle<T>(
    String id,
    Metric<T> metric,
    MetricRenderer<T> perAlgoRenderer,
    ComparisonRenderer<T> comparisonRenderer) {}
