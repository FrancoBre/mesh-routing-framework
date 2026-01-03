package org.ungs.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.ungs.metrics.Metric;
import org.ungs.routing.AlgorithmType;
import org.ungs.routing.RoutingApplicationLoader;
import org.ungs.util.DeterministicRng;

@Slf4j
public class Simulation {

  public static Double TIME = 0.0;
  public static DeterministicRng RANDOM;

  private final SimulationConfig config;
  private final Network network;
  private final Scheduler scheduler;
  private final Registry registry;

  public Simulation(SimulationConfig config, Network network) {
    this.config = config;
    this.network = network;
    this.scheduler = Scheduler.getInstance();
    this.registry = Registry.getInstance();
    RANDOM = new DeterministicRng(config.seed());
  }

  public void run() {

    for (AlgorithmType algorithm : config.algorithms()) {

      log.info("[time={}] Running simulation with algorithm: {}", Simulation.TIME, algorithm);

      // register metrics with labels
      for (Metric<?> metric : registry.getMetrics()) {

        var seriesName =
            algorithm.name()
                + "-"
                + metric.getClass().getSimpleName()
                + "-"
                + config.totalPackets()
                + "PACKETS"
                + "-"
                + config.packetInjectGap()
                + "GAP";

        this.registry.addLabeledMetric(seriesName, metric);

        this.registry.setCurrentMetricLabel(seriesName);
      }

      // install routing applications to nodes
      for (Node node : network.getNodes()) {

        var routingApplication = RoutingApplicationLoader.createRoutingApplication(algorithm, node);

        log.debug(
            "[time={}] Installing routing application {} on node {}",
            Simulation.TIME,
            routingApplication.getClass().getSimpleName(),
            node.getId());
        node.installApplication(routingApplication);
      }

      // create packets
      var origin = network.getNodes().get(0).getId();
      var destination = network.getNodes().get(network.getNodes().size() - 1).getId();

      List<Packet> packets =
          new ArrayList<>(
              IntStream.range(0, config.totalPackets())
                  .mapToObj(i -> new Packet(new Packet.Id(i), origin, destination))
                  .toList());

      registry.registerActivePackets(packets);

      // run simulation until all packets are received
      while (!registry.allPacketsReceived()) {

        // inject packets into the network at the origin node between gaps of ticks
        if (!packets.isEmpty() && Simulation.TIME % config.packetInjectGap() == 0) {

          Packet packetToInject = packets.remove(0);
          log.info(
              "[time={}]: Injecting packet {} into the network at Node {}",
              Simulation.TIME,
              packetToInject.getId(),
              origin);
          network.getNode(origin).receivePacket(packetToInject);
        }

        tick();
      }

      registry.plotMetrics();
    }
  }

  private void tick() {
    log.debug("[time={}] Simulation tick\n\n", Simulation.TIME);

    for (Node node : network.getNodes()) {
      node.getApplication().onTick();
    }

    network.getNodes().stream()
        .flatMap(node -> node.getQueue().stream())
        .forEach(Packet::incrementTimeInQueue);

    scheduler
        .flushPendingSends()
        .forEach(
            (p) -> {
              network.sendPacket(p.from(), p.to(), p.packet());
            });

    registry.collectMetrics();
    Simulation.TIME++;
  }
}
