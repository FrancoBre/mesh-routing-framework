package org.ungs.core.observability.output.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Network;
import org.ungs.core.observability.api.SimulationEvent;
import org.ungs.core.observability.api.SimulationObserver;
import org.ungs.core.observability.events.HopEvent;
import org.ungs.core.observability.events.PacketDeliveredEvent;
import org.ungs.core.observability.output.render.PolicySummaryRenderer;
import org.ungs.core.routing.api.AlgorithmType;

public final class PolicySummaryOutputObserver implements SimulationObserver {

  private final Network network;
  private final Path outDir;
  private final PolicySummaryRenderer renderer = new PolicySummaryRenderer();

  private final List<HopEvent> hops = new ArrayList<>();
  private final List<PacketDeliveredEvent> deliveredEvents = new ArrayList<>();

  public PolicySummaryOutputObserver(Network network, Path outDir) {
    this.network = network;
    this.outDir = outDir;
  }

  @Override
  public void onAlgorithmStart(SimulationRuntimeContext ctx) {
    hops.clear();
    deliveredEvents.clear();
  }

  @Override
  public void onEvent(SimulationEvent event, SimulationRuntimeContext ctx) {
    if (event instanceof HopEvent h) {
      hops.add(h);
    } else if (event instanceof PacketDeliveredEvent d) {
      deliveredEvents.add(d);
    }
  }

  @Override
  public void onAlgorithmEnd(SimulationRuntimeContext ctx) {
    AlgorithmType algo = ctx.getCurrentAlgorithm();

    Path outFile = outDir.resolve(algo.name()).resolve("outputs").resolve("policy_summary.png");
    renderer.render(network, hops, deliveredEvents, algo, outFile);
  }
}
