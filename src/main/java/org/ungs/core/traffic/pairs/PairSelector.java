package org.ungs.core.traffic.pairs;

import org.ungs.core.engine.SimulationRuntimeContext;

public interface PairSelector {

  NodePair pickPair(SimulationRuntimeContext ctx);
}
