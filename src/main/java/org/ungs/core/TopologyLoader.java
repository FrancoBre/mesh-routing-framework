package org.ungs.core;

import java.util.*;

public final class TopologyLoader {

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
