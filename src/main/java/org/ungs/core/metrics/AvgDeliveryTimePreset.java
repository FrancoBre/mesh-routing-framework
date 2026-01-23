package org.ungs.core.metrics;

import java.util.List;
import org.ungs.core.metrics.avgdelivery.AvgDeliveryTimeMetric;
import org.ungs.util.Tuple;

public final class AvgDeliveryTimePreset implements MetricPreset {

  @Override
  public MetricType type() {
    return MetricType.AVG_DELIVERY_TIME;
  }

  @Override
  public Metric<List<Tuple<Double, Double>>> createMetric() {
    return new AvgDeliveryTimeMetric();
  }
}
