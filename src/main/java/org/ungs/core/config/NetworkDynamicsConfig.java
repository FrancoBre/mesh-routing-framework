package org.ungs.core.config;

import org.ungs.cli.SimulationConfigLoader;
import org.ungs.core.dynamics.NetworkDynamicsType;

public sealed interface NetworkDynamicsConfig
    permits NetworkDynamicsConfig.None, NetworkDynamicsConfig.NodeFailures {

    NetworkDynamicsType type();

    record None() implements NetworkDynamicsConfig {
        @Override public NetworkDynamicsType type() { return NetworkDynamicsType.NONE; }
    }

    record NodeFailures(
        String model,
        double p,
        int meanDowntimeTicks,
        int meanUptimeTicks
    ) implements NetworkDynamicsConfig {
        @Override public NetworkDynamicsType type() { return NetworkDynamicsType.NODE_FAILURES; }
    }

    static NetworkDynamicsConfig fromLoader(SimulationConfigLoader l) {
        NetworkDynamicsType type =
            SimulationConfigContext.parseEnum(l.networkDynamics(), NetworkDynamicsType.class);

        return switch (type) {
            case NONE -> new None();
            case NODE_FAILURES -> {
                double p = l.nodeFailuresRandomP();
                if (p < 0.0 || p > 1.0) throw new IllegalArgumentException("network-dynamics.node-failures.random.p must be in [0,1]");
                int down = l.nodeFailuresMeanDowntimeTicks();
                int up = l.nodeFailuresMeanUptimeTicks();
                if (down <= 0 || up <= 0) throw new IllegalArgumentException("mean downtime/uptime must be > 0");
                yield new NodeFailures(
                    l.nodeFailuresModel(),
                    p,
                    down,
                    up
                );
            }
            case LINK_FAILURES, MOBILITY -> throw new IllegalArgumentException("network-dynamics=" + type + " is not implemented yet");
        };
    }
}
