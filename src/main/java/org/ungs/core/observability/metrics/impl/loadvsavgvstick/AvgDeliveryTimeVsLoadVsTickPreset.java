package org.ungs.core.observability.metrics.impl.loadvsavgvstick;

import java.util.List;
import java.util.Optional;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.network.Network;
import org.ungs.core.observability.metrics.api.MetricBundle;
import org.ungs.core.observability.metrics.api.MetricPreset;
import org.ungs.core.observability.metrics.api.MetricType;
import org.ungs.util.Tuple3;

public final class AvgDeliveryTimeVsLoadVsTickPreset
    implements MetricPreset<List<Tuple3<Long, Double, Double>>> {

  @Override
  public MetricType type() {
    return MetricType.AVG_DELIVERY_TIME_VS_LOAD_LEVEL_VS_TICK;
  }

  @Override
  public MetricBundle<List<Tuple3<Long, Double, Double>>> createBundle(
      SimulationConfigContext simCfg, Network network) {

    long warmup = Optional.of(simCfg.general().warmupTicks()).orElse(0);
    int sampleEvery = Optional.of(simCfg.observability().outputSampleEveryTicks()).orElse(1);

    var metric = new AvgDeliveryTimeVsLoadVsTickMetric(warmup, sampleEvery, 500);

    var perAlgoRenderer = new AvgDeliveryTimeVsLoadVsTickLevelRenderer();
    var comparisonRenderer = new AvgDeliveryTimeVsLoadLevelVsTickComparisonRenderer();

    return new MetricBundle<>(
        MetricType.AVG_DELIVERY_TIME_VS_LOAD_LEVEL_VS_TICK.name(),
        metric,
        perAlgoRenderer,
        comparisonRenderer);
  }
}
