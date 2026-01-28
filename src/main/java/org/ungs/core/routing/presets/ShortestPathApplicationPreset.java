package org.ungs.core.routing.presets;

import org.ungs.core.network.Node;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.core.routing.api.RoutingApplication;
import org.ungs.core.routing.api.RoutingApplicationPreset;
import org.ungs.core.routing.impl.shortestpath.ShortestPathApplication;

public final class ShortestPathApplicationPreset implements RoutingApplicationPreset {

  @Override
  public AlgorithmType type() {
    return AlgorithmType.SHORTEST_PATH;
  }

  @Override
  public RoutingApplication createRoutingApplication(Node node) {
    return new ShortestPathApplication(node);
  }
}
