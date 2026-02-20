package org.ungs.core.observability.output.impl;

import java.nio.file.Path;
import java.util.OptionalLong;
import org.ungs.core.config.ObservabilityConfig;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.network.Network;
import org.ungs.core.observability.output.api.OutputBundle;
import org.ungs.core.observability.output.api.OutputPreset;
import org.ungs.core.observability.output.api.OutputType;
import org.ungs.core.observability.route.RouteRecorderObserver;

public final class HeatmapOutputPreset implements OutputPreset {

  @Override
  public OutputType type() {
    return OutputType.HEAT_MAP;
  }

  @Override
  public OutputBundle createBundle(
      SimulationConfigContext simCfg, Network network, RouteRecorderObserver route, Path outDir) {
    ObservabilityConfig obsCfg = simCfg.observability();
    long fromTick = obsCfg.heatmapFromTick();
    OptionalLong toTick = obsCfg.heatmapToTick();

    return new OutputBundle(
        OutputType.HEAT_MAP.name(),
        new RouteHeatmapOutputObserver(network, route, outDir, fromTick, toTick));
  }
}
