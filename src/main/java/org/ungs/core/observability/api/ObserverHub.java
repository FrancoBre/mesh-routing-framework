package org.ungs.core.observability.api;

import org.ungs.core.engine.SimulationRuntimeContext;

public interface ObserverHub extends EventSink {

  void onSimulationStart(SimulationRuntimeContext ctx);

  default void onAlgorithmStart(SimulationRuntimeContext ctx) {}

  default void onAlgorithmEnd(SimulationRuntimeContext ctx) {}

  void onSimulationEnd(SimulationRuntimeContext ctx);
}
