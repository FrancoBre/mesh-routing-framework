package org.ungs.core.termination.presets;

import org.ungs.core.config.TerminationConfig;
import org.ungs.core.termination.api.TerminationPolicy;
import org.ungs.core.termination.api.TerminationPolicyType;

public final class FixedTicksTerminationPreset implements TerminationPolicyPreset {

  @Override
  public TerminationPolicyType type() {
    return TerminationPolicyType.FIXED_TICKS;
  }

  @Override
  public TerminationPolicy create(TerminationConfig config) {
    var c = (TerminationConfig.FixedTicks) config;
    long totalTicks = c.totalTicks();

    return ctx -> ctx.getTick() >= totalTicks;
  }
}
