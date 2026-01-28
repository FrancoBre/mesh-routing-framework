package org.ungs.core.observability.api;

import org.ungs.core.engine.SimulationRuntimeContext;

public interface SimulationObserver {

  default void onAlgorithmStart(SimulationRuntimeContext ctx) {}

  default void onAlgorithmEnd(SimulationRuntimeContext ctx) {}

  default void onSimulationEnd(SimulationRuntimeContext ctx) {}

  default void onEvent(SimulationEvent event, SimulationRuntimeContext ctx) {}
}
