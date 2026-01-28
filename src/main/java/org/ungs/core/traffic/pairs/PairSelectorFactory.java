package org.ungs.core.traffic.pairs;

import java.util.EnumMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.ungs.core.config.PairSelectionConfig;
import org.ungs.core.traffic.runtime.TrafficBuildContext;

@UtilityClass
public final class PairSelectorFactory {

  private static final Map<PairSelectionType, PairSelectorPreset> registry =
      new EnumMap<>(PairSelectionType.class);

  static {
    register(new RandomPairSelectorPreset());
    register(new RandomInGroupsPairSelectorPreset());
    register(new OscillatingBetweenGroupsPairSelectorPreset());
  }

  private static void register(PairSelectorPreset preset) {
    registry.put(preset.type(), preset);
  }

  public static PairSelector from(PairSelectionConfig cfg, TrafficBuildContext ctx) {
    PairSelectorPreset preset = registry.get(cfg.type());
    if (preset == null) throw new IllegalArgumentException("Unknown pair selection: " + cfg.type());
    return preset.create(cfg, ctx);
  }
}
