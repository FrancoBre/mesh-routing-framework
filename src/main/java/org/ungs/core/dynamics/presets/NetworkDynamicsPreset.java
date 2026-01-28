package org.ungs.core.dynamics.presets;

import org.ungs.core.config.NetworkDynamicsConfig;
import org.ungs.core.dynamics.api.NetworkDynamics;
import org.ungs.core.dynamics.api.NetworkDynamicsType;

public sealed interface NetworkDynamicsPreset permits NoOpDynamicsPreset {
  NetworkDynamicsType type();

  NetworkDynamics create(NetworkDynamicsConfig cfg);
}
