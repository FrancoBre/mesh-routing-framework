package org.ungs.core.observability.metrics.impl.windoweddelivery;

import java.util.List;
import java.util.Optional;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.network.Network;
import org.ungs.core.observability.metrics.api.MetricBundle;
import org.ungs.core.observability.metrics.api.MetricPreset;
import org.ungs.core.observability.metrics.api.MetricType;
import org.ungs.util.Tuple;

public final class WindowedDeliveryTimePreset
    implements MetricPreset<List<Tuple<Double, Double>>> {

  /** Default sliding window size: last 500 packets. */
  private static final int DEFAULT_WINDOW_SIZE = 500;

  @Override
  public MetricType type() {
    return MetricType.WINDOWED_DELIVERY_TIME;
  }

  @Override
  public MetricBundle<List<Tuple<Double, Double>>> createBundle(
      SimulationConfigContext simCfg, Network network) {

    long warmup = Optional.of(simCfg.general().warmupTicks()).orElse(0);
    int sampleEvery = Optional.of(simCfg.observability().outputSampleEveryTicks()).orElse(1);

    var metric = new WindowedDeliveryTimeMetric(warmup, sampleEvery, DEFAULT_WINDOW_SIZE);

    var perAlgoRenderer = new WindowedDeliveryTimeRenderer();
    var comparisonRenderer = new WindowedDeliveryTimeComparisonRenderer();

    return new MetricBundle(
        MetricType.WINDOWED_DELIVERY_TIME.name(),
        metric,
        perAlgoRenderer,
        comparisonRenderer);
  }
}
