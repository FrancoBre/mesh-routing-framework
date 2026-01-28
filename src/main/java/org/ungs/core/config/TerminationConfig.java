package org.ungs.core.config;

import java.util.List;
import java.util.OptionalLong;
import org.ungs.cli.SimulationConfigLoader;
import org.ungs.core.termination.api.CompositeMode;
import org.ungs.core.termination.api.TerminationPolicyType;

public sealed interface TerminationConfig
    permits TerminationConfig.FixedTicks,
        TerminationConfig.TotalPacketsDelivered,
        TerminationConfig.Composite {

  TerminationPolicyType type();

  record FixedTicks(long totalTicks) implements TerminationConfig {
    @Override
    public TerminationPolicyType type() {
      return TerminationPolicyType.FIXED_TICKS;
    }
  }

  record TotalPacketsDelivered(long totalPackets) implements TerminationConfig {
    @Override
    public TerminationPolicyType type() {
      return TerminationPolicyType.TOTAL_PACKETS_DELIVERED;
    }
  }

  record Composite(CompositeMode mode, List<TerminationConfig> policies)
      implements TerminationConfig {
    @Override
    public TerminationPolicyType type() {
      return TerminationPolicyType.COMPOSITE;
    }
  }

  static TerminationConfig fromLoader(SimulationConfigLoader l) {
    TerminationPolicyType type =
        SimulationConfigContext.parseEnum(l.terminationPolicy(), TerminationPolicyType.class);

    return switch (type) {
      case FIXED_TICKS -> {
        long ticks = l.terminationFixedTicksTotalTicks();
        if (ticks <= 0)
          throw new IllegalArgumentException(
              "termination-policy.fixed-ticks.total-ticks must be > 0");
        yield new FixedTicks(ticks);
      }
      case TOTAL_PACKETS_DELIVERED -> {
        OptionalLong v =
            SimulationConfigContext.parseOptionalLong(l.terminationTotalPacketsDelivered());
        if (v.isEmpty() || v.getAsLong() <= 0) {
          throw new IllegalArgumentException(
              "termination-policy.packets-delivered.total-packets must be set and > 0");
        }
        yield new TotalPacketsDelivered(v.getAsLong());
      }
      case COMPOSITE -> {
        CompositeMode mode =
            SimulationConfigContext.parseEnum(l.terminationCompositeMode(), CompositeMode.class);
        List<TerminationPolicyType> policyTypes =
            SimulationConfigContext.parseEnumList(
                l.terminationCompositePolicies(), TerminationPolicyType.class);

        if (policyTypes.isEmpty()) {
          throw new IllegalArgumentException(
              "termination-policy.composite.policies must not be empty when COMPOSITE is used");
        }

        // Build each child policy from the same loader (simple approach)
        // (In composite list you typically allow FIXED_TICKS and/or TOTAL_PACKETS_DELIVERED only.)
        List<TerminationConfig> policies =
            policyTypes.stream()
                .filter(t -> t != TerminationPolicyType.COMPOSITE)
                .map(
                    t ->
                        switch (t) {
                          case FIXED_TICKS -> new FixedTicks(l.terminationFixedTicksTotalTicks());
                          case TOTAL_PACKETS_DELIVERED -> {
                            OptionalLong v =
                                SimulationConfigContext.parseOptionalLong(
                                    l.terminationTotalPacketsDelivered());
                            if (v.isEmpty())
                              throw new IllegalArgumentException(
                                  "termination-policy.packets-delivered.total-packets must be set when TOTAL_PACKETS_DELIVERED is used in COMPOSITE");
                            yield new TotalPacketsDelivered(v.getAsLong());
                          }
                          case COMPOSITE ->
                              throw new IllegalStateException("Nested COMPOSITE is not supported");
                        })
                .map(TerminationConfig.class::cast)
                .toList();

        if (policies.isEmpty()) {
          throw new IllegalArgumentException(
              "termination-policy.composite.policies must include at least one non-COMPOSITE policy");
        }

        yield new Composite(mode, policies);
      }
    };
  }
}
