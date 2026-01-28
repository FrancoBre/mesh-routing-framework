package org.ungs.core.observability.metrics.api;

import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.network.Network;

public interface MetricPreset<T> {

  MetricType type();

  MetricBundle<T> createBundle(SimulationConfigContext simCfg, Network network);
}
