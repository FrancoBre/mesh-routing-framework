package org.ungs.core.dynamics.presets;

import org.ungs.core.config.NetworkDynamicsConfig;
import org.ungs.core.dynamics.api.NetworkDynamics;
import org.ungs.core.dynamics.api.NetworkDynamicsType;
import org.ungs.core.dynamics.impl.ScheduledLinkFailuresDynamics;

public final class ScheduledLinkFailuresDynamicsPreset implements NetworkDynamicsPreset {

  @Override
  public NetworkDynamicsType type() {
    return NetworkDynamicsType.SCHEDULED_LINK_FAILURES;
  }

  @Override
  public NetworkDynamics create(NetworkDynamicsConfig cfg) {
    if (cfg instanceof NetworkDynamicsConfig.ScheduledLinkFailures scheduledCfg) {
      return new ScheduledLinkFailuresDynamics(scheduledCfg);
    }
    throw new IllegalArgumentException(
        "Expected ScheduledLinkFailures config but got: " + cfg.getClass().getSimpleName());
  }
}
