package org.ungs.core.topology.api;

import org.ungs.core.network.Network;

public interface TopologyPreset {

  TopologyType type();

  Network createNetwork();
}
