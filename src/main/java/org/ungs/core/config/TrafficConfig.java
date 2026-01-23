package org.ungs.core.config;

import org.ungs.cli.SimulationConfigLoader;

public record TrafficConfig(
    InjectionScheduleConfig injectionSchedule,
    PairSelectionConfig pairSelection,
    PairConstraintsConfig constraints,
    GroupsConfig groups
) {
    public static TrafficConfig fromLoader(SimulationConfigLoader l) {
        InjectionScheduleConfig schedule = InjectionScheduleConfig.fromLoader(l);
        PairSelectionConfig selector = PairSelectionConfig.fromLoader(l);
        PairConstraintsConfig constraints = PairConstraintsConfig.fromLoader(l);
        GroupsConfig groups = GroupsConfig.fromLoader(l);

        // Validate group references for group-based selectors
        if (selector instanceof PairSelectionConfig.RandomInGroups(String fromGroup, String toGroup)) {
            groups.getOrThrow(fromGroup);
            groups.getOrThrow(toGroup);
        }
        if (selector instanceof PairSelectionConfig.OscillatingBetweenGroups obg) {
            groups.getOrThrow(obg.groupA());
            groups.getOrThrow(obg.groupB());
        }

        return new TrafficConfig(schedule, selector, constraints, groups);
    }
}
