package org.ungs.core.engine;

import java.util.List;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.dynamics.api.NetworkDynamics;
import org.ungs.core.dynamics.factory.NetworkDynamicsFactory;
import org.ungs.core.network.Network;
import org.ungs.core.network.Node;
import org.ungs.core.network.Packet;
import org.ungs.core.observability.api.ObserverHub;
import org.ungs.core.observability.events.TickEvent;
import org.ungs.core.observability.factory.ObserverHubFactory;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.core.routing.factory.RoutingApplicationFactory;
import org.ungs.core.termination.api.TerminationPolicy;
import org.ungs.core.termination.factory.TerminationPolicyFactory;
import org.ungs.core.traffic.runtime.TrafficFactory;
import org.ungs.core.traffic.runtime.TrafficInjector;

public final class SimulationEngine {

  private final SimulationConfigContext cfg;
  private final Network network;

  private final TerminationPolicy terminationPolicy;
  private final TrafficInjector trafficInjector;
  private final ObserverHub observers;
  private final NetworkDynamics dynamics;

  public SimulationEngine(SimulationConfigContext cfg, Network network) {
    this.cfg = cfg;
    this.network = network;

    this.terminationPolicy = TerminationPolicyFactory.from(cfg.termination());
    this.trafficInjector = TrafficFactory.from(cfg, network);
    this.observers = ObserverHubFactory.from(cfg, network);
    this.dynamics = NetworkDynamicsFactory.from(cfg.dynamics());
  }

  public void run() {
    SimulationRuntimeContext ctx = new SimulationRuntimeContext(cfg, network, observers);

    observers.onSimulationStart(ctx);

    for (AlgorithmType algorithm : cfg.general().algorithms()) {

      ctx.reset(algorithm);

      network.setRuntimeContext(ctx);

      dynamics.reset(ctx);

      installRoutingApps(algorithm);

      observers.onAlgorithmStart(ctx);

      while (!terminationPolicy.shouldStop(ctx)) {

        dynamics.beforeTick(ctx);

        trafficInjector.inject(ctx);

        tick(ctx);

        ctx.advanceOneTick();

        dynamics.afterTick(ctx);
      }

      observers.onAlgorithmEnd(ctx);
    }

    observers.onSimulationEnd(ctx);
  }

  private void installRoutingApps(AlgorithmType algorithm) {
    for (Node node : network.getNodes()) {

      var app = RoutingApplicationFactory.createRoutingApplication(algorithm, node);

      node.installApplication(app);

      node.emptyQueue();
    }
  }

  private void tick(SimulationRuntimeContext ctx) {
    for (Node node : network.getNodes()) {

      node.getApplication().onTick(ctx);
    }

    network.getNodes().stream()
        .flatMap(node -> node.getQueue().stream())
        .forEach(Packet::incrementTimeInQueue);

    List<SimulationRuntimeContext.PendingSend> sendsThisTick = ctx.flushPendingSends();

    sendsThisTick.forEach((p) -> network.sendPacket(p.from(), p.to(), p.packet()));

    ctx.advanceOneTick();

    ctx.getEventSink()
        .emit(
            new TickEvent(
                ctx.getTick(),
                ctx.getCurrentAlgorithm(),
                network.packetsInFlight(),
                ctx.getDeliveredPackets().size(),
                sendsThisTick.size()));
  }
}
