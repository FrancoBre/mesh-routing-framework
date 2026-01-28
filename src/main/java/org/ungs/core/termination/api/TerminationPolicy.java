package org.ungs.core.termination.api;

import org.ungs.core.engine.SimulationRuntimeContext;

public interface TerminationPolicy {

  boolean shouldStop(SimulationRuntimeContext ctx);
}
