package org.ungs.core.termination.factory;

import java.util.EnumMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.ungs.core.config.TerminationConfig;
import org.ungs.core.termination.api.TerminationPolicy;
import org.ungs.core.termination.api.TerminationPolicyType;
import org.ungs.core.termination.presets.CompositeTerminationPreset;
import org.ungs.core.termination.presets.FixedTicksTerminationPreset;
import org.ungs.core.termination.presets.TerminationPolicyPreset;
import org.ungs.core.termination.presets.TotalPacketsDeliveredTerminationPreset;

@UtilityClass
public final class TerminationPolicyFactory {

  private static final Map<TerminationPolicyType, TerminationPolicyPreset> registry =
      new EnumMap<>(TerminationPolicyType.class);

  static {
    register(new FixedTicksTerminationPreset());
    register(new TotalPacketsDeliveredTerminationPreset());
    register(new CompositeTerminationPreset());
  }

  private static void register(TerminationPolicyPreset preset) {
    registry.put(preset.type(), preset);
  }

  public static TerminationPolicy from(TerminationConfig cfg) {
    TerminationPolicyPreset preset = registry.get(cfg.type());
    if (preset == null)
      throw new IllegalArgumentException("Unknown termination policy: " + cfg.type());
    return preset.create(cfg);
  }
}
