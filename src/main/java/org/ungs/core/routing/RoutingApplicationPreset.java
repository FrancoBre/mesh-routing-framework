package org.ungs.core.routing;

import org.ungs.core.Node;

public sealed interface RoutingApplicationPreset
    permits QRoutingApplicationPreset, ShortestPathApplicationPreset {

  AlgorithmType type();

  RoutingApplication createRoutingApplication(Node node);
}
