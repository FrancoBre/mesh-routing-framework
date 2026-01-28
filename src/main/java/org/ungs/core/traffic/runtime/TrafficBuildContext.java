package org.ungs.core.traffic.runtime;

import java.util.List;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.network.Network;
import org.ungs.core.network.Node;

public record TrafficBuildContext(
    SimulationConfigContext config, Network network, List<Node.Id> stableNodeIds) {}
