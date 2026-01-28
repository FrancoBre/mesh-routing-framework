package org.ungs.core.dynamics.presets;

import org.ungs.core.config.NetworkDynamicsConfig;
import org.ungs.core.dynamics.api.NetworkDynamics;
import org.ungs.core.dynamics.api.NetworkDynamicsType;

public final class NoOpDynamicsPreset implements NetworkDynamicsPreset {

  private static final NetworkDynamics NOOP = new NetworkDynamics() {};

  @Override
  public NetworkDynamicsType type() {
    return NetworkDynamicsType.NONE;
  }

  @Override
  public NetworkDynamics create(NetworkDynamicsConfig cfg) {
    return NOOP;
  }
}
