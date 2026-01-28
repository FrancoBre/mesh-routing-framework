package org.ungs.core.observability.output.impl;

import java.nio.file.Path;
import java.util.Optional;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.network.Network;
import org.ungs.core.observability.output.api.OutputBundle;
import org.ungs.core.observability.output.api.OutputPreset;
import org.ungs.core.observability.output.api.OutputType;
import org.ungs.core.observability.route.RouteRecorderObserver;

public final class RouteFramesOutputPreset implements OutputPreset {

  @Override
  public OutputType type() {
    return OutputType.ROUTE_FRAMES;
  }

  @Override
  public OutputBundle createBundle(
      SimulationConfigContext simCfg, Network network, RouteRecorderObserver route, Path outDir) {

    long sampleEvery = Optional.of(simCfg.observability().outputSampleEveryTicks()).orElse(1);

    return new OutputBundle(
        OutputType.ROUTE_FRAMES.name(),
        new RouteFramesOutputObserver(network, outDir, sampleEvery));
  }
}
