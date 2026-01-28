package org.ungs.core.routing.api;

import org.ungs.core.network.Node;

public interface RoutingApplicationPreset {

  AlgorithmType type();

  RoutingApplication createRoutingApplication(Node node);
}
