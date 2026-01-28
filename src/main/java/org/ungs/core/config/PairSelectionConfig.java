package org.ungs.core.config;

import org.ungs.cli.SimulationConfigLoader;
import org.ungs.core.traffic.pairs.PairSelectionType;

public sealed interface PairSelectionConfig
    permits PairSelectionConfig.Random,
        PairSelectionConfig.RandomInGroups,
        PairSelectionConfig.OscillatingBetweenGroups {

  PairSelectionType type();

  record Random() implements PairSelectionConfig {
    @Override
    public PairSelectionType type() {
      return PairSelectionType.RANDOM;
    }
  }

  record RandomInGroups(String fromGroup, String toGroup) implements PairSelectionConfig {
    @Override
    public PairSelectionType type() {
      return PairSelectionType.RANDOM_IN_GROUPS;
    }
  }

  record OscillatingBetweenGroups(String groupA, String groupB, int periodTicks)
      implements PairSelectionConfig {
    @Override
    public PairSelectionType type() {
      return PairSelectionType.OSCILLATING_BETWEEN_GROUPS;
    }
  }

  static PairSelectionConfig fromLoader(SimulationConfigLoader l) {
    PairSelectionType type =
        SimulationConfigContext.parseEnum(l.pairSelection(), PairSelectionType.class);

    return switch (type) {
      case RANDOM -> new Random();
      case RANDOM_IN_GROUPS -> {
        String from = l.pairSelectionFromGroup();
        String to = l.pairSelectionToGroup();
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
          throw new IllegalArgumentException(
              "pair-selection.groups.from and pair-selection.groups.to are mandatory when pair-selection=RANDOM_IN_GROUPS");
        }
        yield new RandomInGroups(from.trim(), to.trim());
      }
      case OSCILLATING_BETWEEN_GROUPS -> {
        String a = l.pairSelectionOscillatingGroupA();
        String b = l.pairSelectionOscillatingGroupB();
        if (a == null || a.isBlank() || b == null || b.isBlank()) {
          throw new IllegalArgumentException(
              "pair-selection.oscillating.groups.a and .b are mandatory when pair-selection=OSCILLATING_BETWEEN_GROUPS");
        }
        int period = l.pairSelectionOscillatingPeriodTicks();
        if (period <= 0)
          throw new IllegalArgumentException("pair-selection.oscillating.period-ticks must be > 0");
        yield new OscillatingBetweenGroups(a.trim(), b.trim(), period);
      }
    };
  }
}
