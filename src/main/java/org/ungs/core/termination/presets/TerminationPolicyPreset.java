package org.ungs.core.termination.presets;

import org.ungs.core.config.TerminationConfig;
import org.ungs.core.termination.api.TerminationPolicy;
import org.ungs.core.termination.api.TerminationPolicyType;

public sealed interface TerminationPolicyPreset
    permits FixedTicksTerminationPreset,
        TotalPacketsDeliveredTerminationPreset,
        CompositeTerminationPreset {

  TerminationPolicyType type();

  TerminationPolicy create(TerminationConfig config);
}
