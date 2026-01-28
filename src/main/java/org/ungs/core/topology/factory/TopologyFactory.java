package org.ungs.core.topology.factory;

import java.util.*;
import org.ungs.core.network.Network;
import org.ungs.core.topology.api.TopologyPreset;
import org.ungs.core.topology.api.TopologyType;
import org.ungs.core.topology.presets.Grid6x6Preset;

public final class TopologyFactory {

  private static final Map<TopologyType, TopologyPreset> registry =
      new EnumMap<>(TopologyType.class);

  static {
    register(new Grid6x6Preset());
  }

  private static void register(TopologyPreset preset) {
    registry.put(preset.type(), preset);
  }

  public static TopologyPreset getPreset(TopologyType type) {
    TopologyPreset preset = registry.get(type);
    if (preset == null) {
      throw new IllegalArgumentException("Unknown topology type: " + type);
    }
    return preset;
  }

  public static Network createNetwork(TopologyType type) {
    return getPreset(type).createNetwork();
  }
}
