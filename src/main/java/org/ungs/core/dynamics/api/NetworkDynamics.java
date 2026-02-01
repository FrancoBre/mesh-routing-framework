package org.ungs.core.dynamics.api;

import org.ungs.core.engine.SimulationRuntimeContext;

public interface NetworkDynamics {

  /**
   * Called before each algorithm run to reset internal state.
   * This ensures the dynamics behave consistently across multiple algorithm executions.
   *
   * @param ctx the simulation runtime context (with the network to restore if needed)
   */
  default void reset(SimulationRuntimeContext ctx) {}

  default void beforeTick(SimulationRuntimeContext ctx) {}

  default void afterTick(SimulationRuntimeContext ctx) {}
}
