package org.ungs.core.routing.presets;

import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Node;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.core.routing.api.RoutingApplication;
import org.ungs.core.routing.api.RoutingApplicationPreset;
import org.ungs.core.routing.impl.fullecho.FullEchoQRoutingApplication;

public final class FullEchoQRoutingApplicationPreset implements RoutingApplicationPreset {

  @Override
  public AlgorithmType type() {
    return AlgorithmType.FULL_ECHO_Q_ROUTING;
  }

  @Override
  public RoutingApplication createRoutingApplication(Node node, SimulationRuntimeContext ctx) {
    return new FullEchoQRoutingApplication(node, ctx);
  }
}
