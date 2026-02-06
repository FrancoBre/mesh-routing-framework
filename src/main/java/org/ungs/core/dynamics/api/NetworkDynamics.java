package org.ungs.core.dynamics.api;

import org.ungs.core.engine.SimulationRuntimeContext;

public interface NetworkDynamics {

  default void beforeTick(SimulationRuntimeContext ctx) {}

  default void afterTick(SimulationRuntimeContext ctx) {}
}
