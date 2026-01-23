package org.ungs.core.routing;

import org.ungs.core.Node;
import org.ungs.core.routing.shortestpath.ShortestPathApplication;

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
