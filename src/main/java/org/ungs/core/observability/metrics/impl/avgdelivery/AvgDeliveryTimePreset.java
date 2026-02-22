package org.ungs.core.observability.metrics.impl.avgdelivery;

import java.util.List;
import java.util.Optional;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.network.Network;
import org.ungs.core.observability.metrics.api.MetricBundle;
import org.ungs.core.observability.metrics.api.MetricPreset;
import org.ungs.core.observability.metrics.api.MetricType;
import org.ungs.util.Tuple;

public final class AvgDeliveryTimePreset implements MetricPreset<List<Tuple<Double, Double>>> {

  @Override
  public MetricType type() {
    return MetricType.AVG_DELIVERY_TIME;
  }

  @Override
  public MetricBundle<List<Tuple<Double, Double>>> createBundle(
      SimulationConfigContext simCfg, Network network) {
    long warmup = Optional.of(simCfg.general().warmupTicks()).orElse(0);
    int sampleEvery = Optional.of(simCfg.observability().outputSampleEveryTicks()).orElse(1);
    int windowSize = simCfg.observability().metricWindowSize();

    var metric = new AvgDeliveryTimeMetric(warmup, sampleEvery, windowSize);

    var perAlgoRenderer = new AvgDeliveryTimeRenderer();
    var comparisonRenderer = new AvgDeliveryTimeComparisonRenderer();

    return new MetricBundle(
        MetricType.AVG_DELIVERY_TIME.name(), metric, perAlgoRenderer, comparisonRenderer);
  }
}
