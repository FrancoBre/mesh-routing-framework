package org.ungs.core.observability.output.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Network;
import org.ungs.core.observability.api.SimulationEvent;
import org.ungs.core.observability.api.SimulationObserver;
import org.ungs.core.observability.events.HopEvent;
import org.ungs.core.observability.events.TickEvent;
import org.ungs.core.observability.output.render.RouteFrameRenderer;
import org.ungs.core.routing.api.AlgorithmType;

public final class RouteFramesOutputObserver implements SimulationObserver {

  private final Network network;
  private final Path outDir;
  private final List<HopEvent> receivedHops = new ArrayList<>();

  private final RouteFrameRenderer renderer = new RouteFrameRenderer();

  public RouteFramesOutputObserver(Network network, Path outDir) {
    this.network = network;
    this.outDir = outDir;
  }

  @Override
  public void onEvent(SimulationEvent e, SimulationRuntimeContext ctx) {
    if (e instanceof HopEvent h) {
      receivedHops.add(h);
      return;
    }

    if (e instanceof TickEvent t) {
      long tick = (long) ctx.getTick();

      AlgorithmType algo = t.algorithm();
      List<HopEvent> hopsThisTick =
          receivedHops.stream().filter(h -> h.algorithm().equals(algo)).toList();

      String filename = String.format("tick-%05d.png", tick);
      Path outFile =
          outDir.resolve(algo.name()).resolve("outputs").resolve("frames").resolve(filename);

      renderer.renderTickFrame(network, tick, hopsThisTick, outFile);

      receivedHops.clear();
    }
  }
}
