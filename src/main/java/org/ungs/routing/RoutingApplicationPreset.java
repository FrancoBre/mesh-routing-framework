package org.ungs.routing;

import org.ungs.core.Node;

public sealed interface RoutingApplicationPreset permits QRoutingApplicationPreset {

  AlgorithmType type();

  RoutingApplication createRoutingApplication(Node node);
}
