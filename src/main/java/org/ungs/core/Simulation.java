package org.ungs.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.ungs.metrics.Metric;
import org.ungs.routing.AlgorithmType;
import org.ungs.routing.RoutingApplicationLoader;
import org.ungs.util.DeterministicRng;
import org.ungs.util.PlateauConstants;
import org.ungs.util.RouteVisualizer;

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

      registry.reset();
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

      int incrementalCount = 1;
      int currentBatchSize = PlateauConstants.RAMP_START_BATCH_SIZE;
      // run simulation until all packets are received
      while (!registry.allPacketsReceived()) {

        // inject packets into the network at the origin node between gaps of ticks
        if (!packets.isEmpty()) {
          switch (TrafficInjectionMode.getByConfig(config)) {
            case ALL_AT_ONCE -> {
              while (!packets.isEmpty()) {
                Packet packetToInject = packets.remove(0);
                log.info(
                    "[time={}]: Injecting packet {} into the network at Node {}",
                    Simulation.TIME,
                    packetToInject.getId(),
                    origin);
                packetToInject.markAsDeparted();
                network.getNode(origin).receivePacket(packetToInject);
              }
            }
            case CONSTANT_GAP -> {
              if (Simulation.TIME % config.packetInjectGap() == 0) {
                Packet packetToInject = packets.remove(0);
                log.info(
                    "[time={}]: Injecting packet {} into the network at Node {}",
                    Simulation.TIME,
                    packetToInject.getId(),
                    origin);
                packetToInject.markAsDeparted();
                network.getNode(origin).receivePacket(packetToInject);
              }
            }
            case LINEAR_INCREMENTAL -> {
              int toInject = Math.min(incrementalCount, packets.size());
              for (int i = 0; i < toInject; i++) {
                Packet packetToInject = packets.remove(0);
                log.info(
                    "[time={}]: Injecting packet {} into the network at Node {}",
                    Simulation.TIME,
                    packetToInject.getId(),
                    origin);
                packetToInject.markAsDeparted();
                network.getNode(origin).receivePacket(packetToInject);
              }
              incrementalCount++;
            }

            case PLATEAU_THEN_LINEAR -> {
              if (Simulation.TIME < PlateauConstants.PLATEAU_TICKS) {
                // --------- PHASE 1: plateau constante ----------
                if (Simulation.TIME % PlateauConstants.PLATEAU_INJECT_EVERY_N_TICKS == 0) {
                  int toInject = Math.min(PlateauConstants.PLATEAU_BATCH_SIZE, packets.size());
                  for (int i = 0; i < toInject; i++) {
                    Packet p = packets.remove(0);
                    log.info(
                        "[time={}]: Injecting packet {} into the network at Node {}",
                        Simulation.TIME,
                        p.getId(),
                        origin);
                    p.markAsDeparted();
                    network.getNode(origin).receivePacket(p);
                  }
                }

              } else {
                // --------- PHASE 2: rampa lineal ----------
                // subí la carga de a +1 cada rampIncreaseEveryNTicks
                if ((Simulation.TIME - PlateauConstants.PLATEAU_TICKS)
                        % PlateauConstants.RAMP_INCREASE_EVERY_N_TICKS
                    == 0) {
                  currentBatchSize++;
                }

                if (Simulation.TIME % PlateauConstants.RAMP_INJECT_EVERY_N_TICKS == 0) {
                  int toInject = Math.min(currentBatchSize, packets.size());
                  for (int i = 0; i < toInject; i++) {
                    Packet p = packets.remove(0);
                    log.info(
                        "[time={}]: Injecting packet {} into the network at Node {}",
                        Simulation.TIME,
                        p.getId(),
                        origin);
                    p.markAsDeparted();
                    network.getNode(origin).receivePacket(p);
                  }
                }
              }
            }
          }
          tick();
        } else {
          tick();
        }
      }
      registry.plotEverything(config, algorithm);
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

    if (Simulation.TIME % 10 == 0) {
      int maxQueueSize =
          network.getNodes().stream().mapToInt(node -> node.getQueue().size()).max().orElse(0);
      log.info("[time={}]: Max queue size across all nodes: {}", Simulation.TIME, maxQueueSize);
    }

    List<Scheduler.PendingSend> sendsThisTick = scheduler.flushPendingSends();

    RouteVisualizer.saveTickFramePng(network, Simulation.TIME, sendsThisTick);

    sendsThisTick.forEach((p) -> network.sendPacket(p.from(), p.to(), p.packet()));

    registry.collectMetrics();
    Simulation.TIME++;
  }

  enum TrafficInjectionMode {
    CONSTANT_GAP,
    ALL_AT_ONCE,
    LINEAR_INCREMENTAL,
    PLATEAU_THEN_LINEAR;

    public static TrafficInjectionMode getByConfig(SimulationConfig config) {
      if (config.linearIncrementalPacketInjection()) {
        return TrafficInjectionMode.LINEAR_INCREMENTAL;
      }
      if (config.plateauThenLinearPacketInjection()) {
        return TrafficInjectionMode.PLATEAU_THEN_LINEAR;
      }
      if (config.packetInjectGap() > 0) {
        return TrafficInjectionMode.CONSTANT_GAP;
      }
      if (config.packetInjectGap() == 0) {
        return TrafficInjectionMode.ALL_AT_ONCE;
      }
      throw new IllegalArgumentException("Invalid traffic injection configuration");
    }
  }
}
