package org.ungs.routing;

import java.util.EnumMap;
import java.util.Map;
import org.ungs.core.Node;

public final class RoutingApplicationLoader {

  private static final Map<AlgorithmType, RoutingApplicationPreset> registry =
      new EnumMap<>(AlgorithmType.class);

  static {
    register(new QRoutingApplicationPreset());
  }

  private static void register(RoutingApplicationPreset preset) {
    registry.put(preset.type(), preset);
  }

  public static RoutingApplicationPreset getPreset(AlgorithmType type) {
    RoutingApplicationPreset preset = registry.get(type);
    if (preset == null) {
      throw new IllegalArgumentException("Unknown algorithm type: " + type);
    }
    return preset;
  }

  public static RoutingApplication createRoutingApplication(AlgorithmType type, Node node) {
    return getPreset(type).createRoutingApplication(node);
  }
}
