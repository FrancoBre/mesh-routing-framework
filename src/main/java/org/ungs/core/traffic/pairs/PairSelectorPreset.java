package org.ungs.core.traffic.pairs;

import org.ungs.core.config.PairSelectionConfig;
import org.ungs.core.traffic.runtime.TrafficBuildContext;

public sealed interface PairSelectorPreset
    permits RandomPairSelectorPreset,
        RandomInGroupsPairSelectorPreset,
        OscillatingBetweenGroupsPairSelectorPreset {

  PairSelectionType type();

  PairSelector create(PairSelectionConfig cfg, TrafficBuildContext ctx);
}
