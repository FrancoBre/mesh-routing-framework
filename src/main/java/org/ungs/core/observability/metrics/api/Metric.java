package org.ungs.core.observability.metrics.api;

import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.observability.api.SimulationEvent;

public interface Metric<T> {

  void reset();

  default void onEvent(SimulationEvent e, SimulationRuntimeContext ctx) {}

  T snapshot();
}
