package org.ungs.core.routing.factory;

import java.util.EnumMap;
import java.util.Map;
import org.ungs.core.network.Node;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.core.routing.api.RoutingApplication;
import org.ungs.core.routing.api.RoutingApplicationPreset;
import org.ungs.core.routing.presets.FullEchoQRoutingApplicationPreset;
import org.ungs.core.routing.presets.QRoutingApplicationPreset;
import org.ungs.core.routing.presets.ShortestPathApplicationPreset;

public final class RoutingApplicationFactory {

  private static final Map<AlgorithmType, RoutingApplicationPreset> registry =
      new EnumMap<>(AlgorithmType.class);

  static {
    register(new QRoutingApplicationPreset());
    register(new ShortestPathApplicationPreset());
    register(new FullEchoQRoutingApplicationPreset());
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
