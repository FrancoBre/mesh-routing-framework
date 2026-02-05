package org.ungs.core.observability.metrics.impl.loadvsavg;

import java.util.List;
import java.util.Optional;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.network.Network;
import org.ungs.core.observability.events.LoadLevelUpdatedEvent;
import org.ungs.core.observability.metrics.api.MetricBundle;
import org.ungs.core.observability.metrics.api.MetricPreset;
import org.ungs.core.observability.metrics.api.MetricType;
import org.ungs.util.Tuple3;

public final class AvgDeliveryTimeVsLoadLevelPreset
    implements MetricPreset<List<Tuple3<Double, Double, LoadLevelUpdatedEvent.LoadLevelTrend>>> {

  @Override
  public MetricType type() {
    return MetricType.AVG_DELIVERY_TIME_VS_LOAD_LEVEL;
  }

  @Override
  public MetricBundle<List<Tuple3<Double, Double, LoadLevelUpdatedEvent.LoadLevelTrend>>>
      createBundle(SimulationConfigContext simCfg, Network network) {

    long warmup = Optional.of(simCfg.general().warmupTicks()).orElse(0);
    int sampleEvery = Optional.of(simCfg.observability().outputSampleEveryTicks()).orElse(1);

    var metric = new AvgDeliveryTimeVsLoadLevelMetric(warmup, sampleEvery);

    var perAlgoRenderer = new AvgDeliveryTimeVsLoadLevelRenderer();
    var comparisonRenderer = new AvgDeliveryTimeVsLoadLevelComparisonRenderer();

    return new MetricBundle<>(
        MetricType.AVG_DELIVERY_TIME_VS_LOAD_LEVEL.name(),
        metric,
        perAlgoRenderer,
        comparisonRenderer);
  }
}
