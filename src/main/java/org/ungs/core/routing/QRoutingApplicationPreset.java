package org.ungs.core.routing;

import org.ungs.core.Node;
import org.ungs.core.routing.qrouting.QRoutingApplication;

public final class QRoutingApplicationPreset implements RoutingApplicationPreset {

  @Override
  public AlgorithmType type() {
    return AlgorithmType.Q_ROUTING;
  }

  @Override
  public RoutingApplication createRoutingApplication(Node node) {
    return new QRoutingApplication(node);
  }
}
