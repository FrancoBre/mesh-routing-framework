package org.ungs.core;

public sealed interface TopologyPreset permits Grid6x6Preset {

  TopologyType type();

  Network createNetwork();
}
