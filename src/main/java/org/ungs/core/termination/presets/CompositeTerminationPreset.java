package org.ungs.core.termination.presets;

import java.util.List;
import org.ungs.core.config.TerminationConfig;
import org.ungs.core.termination.api.TerminationPolicy;
import org.ungs.core.termination.api.TerminationPolicyType;
import org.ungs.core.termination.factory.TerminationPolicyFactory;

public final class CompositeTerminationPreset implements TerminationPolicyPreset {

  @Override
  public TerminationPolicyType type() {
    return TerminationPolicyType.COMPOSITE;
  }

  @Override
  public TerminationPolicy create(TerminationConfig config) {
    var c = (TerminationConfig.Composite) config;

    List<TerminationPolicy> children =
        c.policies().stream().map(TerminationPolicyFactory::from).toList();

    return switch (c.mode()) {
      case OR -> ctx -> children.stream().anyMatch(p -> p.shouldStop(ctx));
      case AND -> ctx -> children.stream().allMatch(p -> p.shouldStop(ctx));
    };
  }
}
