package org.ungs.core.dynamics.factory;

import java.util.EnumMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.ungs.core.config.NetworkDynamicsConfig;
import org.ungs.core.dynamics.api.NetworkDynamics;
import org.ungs.core.dynamics.api.NetworkDynamicsType;
import org.ungs.core.dynamics.presets.NetworkDynamicsPreset;
import org.ungs.core.dynamics.presets.NoOpDynamicsPreset;
import org.ungs.core.dynamics.presets.ScheduledLinkFailuresDynamicsPreset;

@UtilityClass
public final class NetworkDynamicsFactory {

  private static final Map<NetworkDynamicsType, NetworkDynamicsPreset> registry =
      new EnumMap<>(NetworkDynamicsType.class);

  static {
    register(new NoOpDynamicsPreset());
    register(new ScheduledLinkFailuresDynamicsPreset());
    // register(new NodeFailuresDynamicsPreset()); TODO
  }

  private static void register(NetworkDynamicsPreset preset) {
    registry.put(preset.type(), preset);
  }

  public static NetworkDynamics from(NetworkDynamicsConfig cfg) {
    NetworkDynamicsPreset preset = registry.get(cfg.type());
    if (preset == null)
      throw new IllegalArgumentException("Unknown network dynamics: " + cfg.type());
    return preset.create(cfg);
  }
}
