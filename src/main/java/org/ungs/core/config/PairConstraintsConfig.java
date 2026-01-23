package org.ungs.core.config;

import org.ungs.cli.SimulationConfigLoader;

public record PairConstraintsConfig(
    boolean disallowSelf,
    boolean disallowNeighbor
) {
    public static PairConstraintsConfig fromLoader(SimulationConfigLoader l) {
        return new PairConstraintsConfig(
            l.pairSelectionDisallowSelf(),
            l.pairSelectionDisallowNeighbor()
        );
    }
}
