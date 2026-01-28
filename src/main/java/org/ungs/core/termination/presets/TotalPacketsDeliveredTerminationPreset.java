package org.ungs.core.termination.presets;

import org.ungs.core.config.TerminationConfig;
import org.ungs.core.termination.api.TerminationPolicy;
import org.ungs.core.termination.api.TerminationPolicyType;

public final class TotalPacketsDeliveredTerminationPreset implements TerminationPolicyPreset {

  @Override
  public TerminationPolicyType type() {
    return TerminationPolicyType.TOTAL_PACKETS_DELIVERED;
  }

  @Override
  public TerminationPolicy create(TerminationConfig config) {
    var c = (TerminationConfig.TotalPacketsDelivered) config;
    long totalPackets = c.totalPackets();

    return ctx -> ctx.getDeliveredPackets().size() >= totalPackets;
  }
}
