package org.ungs.core.observability.output.impl;

import java.nio.file.Path;
import java.util.List;
import java.util.OptionalLong;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Network;
import org.ungs.core.observability.api.SimulationObserver;
import org.ungs.core.observability.events.HopEvent;
import org.ungs.core.observability.output.render.RouteHeatmapRenderer;
import org.ungs.core.observability.route.RouteRecorderObserver;
import org.ungs.core.routing.api.AlgorithmType;

public final class HeatmapOutputObserver implements SimulationObserver {

  private final Network network;
  private final RouteRecorderObserver route;
  private final Path outDir;
  private final long fromTick;
  private final OptionalLong toTick;
  private final RouteHeatmapRenderer renderer = new RouteHeatmapRenderer();

  public HeatmapOutputObserver(
      Network network,
      RouteRecorderObserver route,
      Path outDir,
      long fromTick,
      OptionalLong toTick) {
    this.network = network;
    this.route = route;
    this.outDir = outDir;
    this.fromTick = fromTick;
    this.toTick = toTick;
  }

  @Override
  public void onAlgorithmEnd(SimulationRuntimeContext ctx) {
    AlgorithmType algo = ctx.getCurrentAlgorithm();

    List<HopEvent> hops = route.snapshot();

    Path outFile = outDir.resolve(algo.name()).resolve("outputs").resolve("route_heatmap.png");
    renderer.render(network, hops, algo, outFile, fromTick, toTick);
  }
}
