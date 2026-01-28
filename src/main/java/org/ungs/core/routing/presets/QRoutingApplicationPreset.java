package org.ungs.core.routing.presets;

import org.ungs.core.network.Node;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.core.routing.api.RoutingApplication;
import org.ungs.core.routing.api.RoutingApplicationPreset;
import org.ungs.core.routing.impl.qrouting.QRoutingApplication;

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
