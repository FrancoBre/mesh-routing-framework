package org.ungs.core.traffic.pairs;

import org.ungs.core.engine.SimulationRuntimeContext;

public interface PairConstraint {

  boolean isAllowed(SimulationRuntimeContext ctx, NodePair pair);
}
