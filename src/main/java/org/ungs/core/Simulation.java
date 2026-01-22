package org.ungs.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.ungs.metrics.Metric;
import org.ungs.routing.AlgorithmType;
import org.ungs.routing.RoutingApplicationLoader;
import org.ungs.util.DeterministicRng;
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

  public void run() throws IOException {

    for (AlgorithmType algorithm : config.algorithms()) {

      Simulation.TIME = 0.0;
      Simulation.RANDOM = new DeterministicRng(config.seed());

      registry.resetAll();
      registry.setCurrentAlgorithm(algorithm);

      log.info("[time={}] Running simulation with algorithm: {}", Simulation.TIME, algorithm);

      // register metrics with labels
      for (Metric<?> metric : registry.getMetrics()) {
        var seriesName =
            algorithm.name()
                + "-"
                + metric.getClass().getSimpleName()
                + "-"
                + config.totalTicks()
                + "TICKS"
                + "-"
                + "LOAD"
                + config.loadLevel();

        this.registry.addLabeledMetric(seriesName, metric);
        this.registry.setCurrentMetricLabel(seriesName);
      }

      // install routing applications to nodes + clear queues
      for (Node node : network.getNodes()) {
        var routingApplication = RoutingApplicationLoader.createRoutingApplication(algorithm, node);
        log.debug(
            "[time={}] Installing routing application {} on node {}",
            Simulation.TIME,
            routingApplication.getClass().getSimpleName(),
            node.getId());
        node.installApplication(routingApplication);
        node.emptyQueue();
      }

      // deterministic node id list (stable order)
      List<Node.Id> nodeIds =
          network.getNodes().stream()
              .map(Node::getId)
              .sorted(Comparator.comparingInt(Node.Id::value))
              .toList();

      int currentPacketId = 0;
      final int MAX_ACTIVE_PACKETS = 1000;

      // run simulation for a fixed number of ticks
      while (Simulation.TIME < config.totalTicks()) {

        // ---- GLOBAL injection per tick, allowing loadLevel > 1 ----
        // injectCount = floor(L) + Bernoulli(frac(L))
        double L = config.loadLevel();
        int base = (int) Math.floor(L);
        double frac = L - base;

        int injectCount = base;
        if (frac > 0.0) {
          double u = RANDOM.nextUnitDouble(); // in [0,1)
          if (u < frac) {
            injectCount += 1;
          }
        }

        // obey max packets in flight (sum of all node queues)
        int availableSlots = MAX_ACTIVE_PACKETS - network.packetsInFlight();
        if (availableSlots <= 0) {
          // no injection this tick
          tick();
          continue;
        }
        injectCount = Math.min(injectCount, availableSlots);

        for (int j = 0; j < injectCount; j++) {

          // random origin
          Node.Id originId = nodeIds.get(RANDOM.nextIndex(nodeIds.size()));

          // random destination != origin nor an immediate neighbor
          Node.Id destinationId;
          do {
            destinationId = nodeIds.get(RANDOM.nextIndex(nodeIds.size()));
          } while (destinationId.equals(originId));

          if (network.isNeighbor(destinationId, originId)) {
            continue;
          }

          Packet packetToInject =
              new Packet(new Packet.Id(currentPacketId++), originId, destinationId);

          log.debug(
              "[time={}]: Injecting packet {} at {} -> {}",
              Simulation.TIME,
              packetToInject.getId(),
              originId,
              destinationId);

          packetToInject.markAsDeparted();
          network.getNode(originId).receivePacket(packetToInject);
        }

        tick();
      }

      registry.plotAlgorithmSpecific(config);
    }

    // optional: overall comparison plot at the end (if you want it)
    // registry.plotEverything(config);
  }

  //  public void run() throws IOException {
  //
  //    int currentPacketId = 0;
  //    for (AlgorithmType algorithm : config.algorithms()) {
  //
  //      Simulation.TIME = 0.0;
  //      Simulation.RANDOM = new DeterministicRng(config.seed());
  //      registry.resetAll();
  //      registry.setCurrentAlgorithm(algorithm);
  //      log.info("[time={}] Running simulation with algorithm: {}", Simulation.TIME, algorithm);
  //
  //      // register metrics with labels
  //      for (Metric<?> metric : registry.getMetrics()) {
  //
  //        var seriesName =
  //            algorithm.name()
  //                + "-"
  //                + metric.getClass().getSimpleName()
  //                + "-"
  //                + config.totalPackets()
  //                + "PACKETS"
  //                + "-"
  //                + config.packetInjectGap()
  //                + "GAP";
  //
  //        this.registry.addLabeledMetric(seriesName, metric);
  //
  //        this.registry.setCurrentMetricLabel(seriesName);
  //      }
  //
  //      // install routing applications to nodes
  //      for (Node node : network.getNodes()) {
  //
  //        var routingApplication = RoutingApplicationLoader.createRoutingApplication(algorithm,
  // node);
  //
  //        log.debug(
  //            "[time={}] Installing routing application {} on node {}",
  //            Simulation.TIME,
  //            routingApplication.getClass().getSimpleName(),
  //            node.getId());
  //        node.installApplication(routingApplication);
  //        node.emptyQueue();
  //      }
  //
  //      // run simulation until total ticks reached
  //      while (config.totalTicks() > Simulation.TIME) {
  //
  //        // case PROB_PER_NODE_PER_TICK -> {
  //
  //        // Precompute ids (deterministic order)
  //        List<Node.Id> nodeIds =
  //            network.getNodes().stream()
  //                .map(Node::getId)
  //                .sorted(Comparator.comparingInt(Node.Id::value))
  //                .toList();
  //
  //        // run simulation for a fixed number of ticks
  //        while (Simulation.TIME < config.totalTicks()) {
  //
  //          // For each node, inject 1 packet with probability loadLevel
  //          for (Node.Id originId : nodeIds) {
  //
  //            // uniform u in [0,1)
  //            double u = RANDOM.nextUnitDouble();
  //            if (u <= config.loadLevel()) {
  //              continue;
  //            }
  //
  //            // pick a destination != origin (deterministic)
  //            Node.Id destinationId;
  //            do {
  //              destinationId = nodeIds.get(RANDOM.nextIndex(nodeIds.size()));
  //            } while (destinationId.equals(originId));
  //
  //            Packet packetToInject = new Packet(new Packet.Id(currentPacketId++), originId,
  // destinationId);
  //
  //            log.info(
  //                "[time={}]: Injecting packet {} into the network at Node {} -> {}",
  //                Simulation.TIME,
  //                packetToInject.getId(),
  //                originId,
  //                destinationId);
  //
  //            packetToInject.markAsDeparted();
  //            network.getNode(originId).receivePacket(packetToInject);
  //          }
  //
  //          tick();
  //          // }
  //        }
  //      }
  //      registry.plotAlgorithmSpecific(config);
  //    }
  ////    registry.plotEverything(config);
  //  }

  //  public void run() throws IOException {
  //
  //    for (AlgorithmType algorithm : config.algorithms()) {
  //
  //      Simulation.TIME = 0.0;
  //      Simulation.RANDOM = new DeterministicRng(config.seed());
  //      registry.resetAll();
  //      registry.setCurrentAlgorithm(algorithm);
  //      log.info("[time={}] Running simulation with algorithm: {}", Simulation.TIME, algorithm);
  //
  //      // register metrics with labels
  //      for (Metric<?> metric : registry.getMetrics()) {
  //
  //        var seriesName =
  //            algorithm.name()
  //                + "-"
  //                + metric.getClass().getSimpleName()
  //                + "-"
  //                + config.totalPackets()
  //                + "PACKETS"
  //                + "-"
  //                + config.packetInjectGap()
  //                + "GAP";
  //
  //        this.registry.addLabeledMetric(seriesName, metric);
  //
  //        this.registry.setCurrentMetricLabel(seriesName);
  //      }
  //
  //      // install routing applications to nodes
  //      for (Node node : network.getNodes()) {
  //
  //        var routingApplication = RoutingApplicationLoader.createRoutingApplication(algorithm,
  // node);
  //
  //        log.debug(
  //            "[time={}] Installing routing application {} on node {}",
  //            Simulation.TIME,
  //            routingApplication.getClass().getSimpleName(),
  //            node.getId());
  //        node.installApplication(routingApplication);
  //      }
  //
  //      // TODO: add packet generation strategies (single source-destination, random, random en
  //      // subconjuntos, oscilante entre dos subconjuntos, etc)
  ////      List<Packet> packets = generatePacketsRandomly(network.getNodes(),
  // config.totalPackets());
  //      List<Packet> packets = generatePacketsLeftToRight(network.getNodes(),
  // config.totalPackets());
  //
  //      registry.registerActivePackets(packets);
  //
  //      int incrementalCount = 1;
  //      int currentBatchSize = PlateauThenLinearConstants.RAMP_START_BATCH_SIZE;
  //      // run simulation until all packets are received
  //      while (!registry.allPacketsReceived()) {
  //
  //        // inject packets into the network at the origin node between gaps of ticks
  //        if (!packets.isEmpty()) {
  //          switch (TrafficInjectionMode.getByConfig(config)) {
  //            case ALL_AT_ONCE -> {
  //              while (!packets.isEmpty()) {
  //                Packet packetToInject = packets.remove(0);
  //                log.info(
  //                    "[time={}]: Injecting packet {} into the network at Node {}",
  //                    Simulation.TIME,
  //                    packetToInject.getId(),
  //                    packetToInject.getOrigin());
  //                packetToInject.markAsDeparted();
  //                network.getNode(packetToInject.getOrigin()).receivePacket(packetToInject);
  //              }
  //            }
  //            case CONSTANT_GAP -> {
  //
  //              // warmup
  //              if (Simulation.TIME < 1000 && Simulation.TIME % 2 == 0) {
  //                for (int i = 0; i < 2; i++) {
  //                  Packet packetToInject = packets.remove(0);
  //                  log.info(
  //                      "[time={}]: Injecting packet {} into the network at Node {}",
  //                      Simulation.TIME,
  //                      packetToInject.getId(),
  //                      packetToInject.getOrigin());
  //                  packetToInject.markAsDeparted();
  //                  network.getNode(packetToInject.getOrigin()).receivePacket(packetToInject);
  //                }
  //              }
  //
  //              //              if (Simulation.TIME % config.packetInjectGap() == 0) {
  //              //                Packet packetToInject = packets.remove(0);
  //              //                log.info(
  //              //                    "[time={}]: Injecting packet {} into the network at Node
  // {}",
  //              //                    Simulation.TIME,
  //              //                    packetToInject.getId(),
  //              //                    origin);
  //              //                packetToInject.markAsDeparted();
  //              //                network.getNode(origin).receivePacket(packetToInject);
  //
  //              for (int i = 0; i < 2; i++) {
  //                Packet packetToInject = packets.remove(0);
  //                log.info(
  //                    "[time={}]: Injecting packet {} into the network at Node {}",
  //                    Simulation.TIME,
  //                    packetToInject.getId(),
  //                    packetToInject.getOrigin());
  //                packetToInject.markAsDeparted();
  //                network.getNode(packetToInject.getOrigin()).receivePacket(packetToInject);
  //              }
  //              //              }
  //            }
  //            case LINEAR_INCREMENTAL -> {
  //              int toInject = Math.min(incrementalCount, packets.size());
  //              for (int i = 0; i < toInject; i++) {
  //                Packet packetToInject = packets.remove(0);
  //                log.info(
  //                    "[time={}]: Injecting packet {} into the network at Node {}",
  //                    Simulation.TIME,
  //                    packetToInject.getId(),
  //                    packetToInject.getOrigin());
  //                packetToInject.markAsDeparted();
  //                network.getNode(packetToInject.getOrigin()).receivePacket(packetToInject);
  //              }
  //              incrementalCount++;
  //            }
  //
  //            case PLATEAU_THEN_LINEAR -> {
  //              if (Simulation.TIME < PlateauThenLinearConstants.PLATEAU_TICKS) {
  //                // --------- PHASE 1: plateau constante ----------
  //                if (Simulation.TIME % PlateauThenLinearConstants.PLATEAU_INJECT_EVERY_N_TICKS
  //                    == 0) {
  //                  int toInject =
  //                      Math.min(PlateauThenLinearConstants.PLATEAU_BATCH_SIZE, packets.size());
  //                  for (int i = 0; i < toInject; i++) {
  //                    Packet p = packets.remove(0);
  //                    log.info(
  //                        "[time={}]: Injecting packet {} into the network at Node {}",
  //                        Simulation.TIME,
  //                        p.getId(),
  //                        p.getOrigin());
  //                    p.markAsDeparted();
  //                    network.getNode(p.getOrigin()).receivePacket(p);
  //                  }
  //                }
  //
  //              } else {
  //                // --------- PHASE 2: rampa lineal ----------
  //                // subí la carga de a +1 cada rampIncreaseEveryNTicks
  //                if ((Simulation.TIME - PlateauThenLinearConstants.PLATEAU_TICKS)
  //                        % PlateauThenLinearConstants.RAMP_INCREASE_EVERY_N_TICKS
  //                    == 0) {
  //                  currentBatchSize++;
  //                }
  //
  //                if (Simulation.TIME % PlateauThenLinearConstants.RAMP_INJECT_EVERY_N_TICKS == 0)
  // {
  //                  int toInject = Math.min(currentBatchSize, packets.size());
  //                  for (int i = 0; i < toInject; i++) {
  //                    Packet p = packets.remove(0);
  //                    log.info(
  //                        "[time={}]: Injecting packet {} into the network at Node {}",
  //                        Simulation.TIME,
  //                        p.getId(),
  //                        p.getOrigin());
  //                    p.markAsDeparted();
  //                    network.getNode(p.getOrigin()).receivePacket(p);
  //                  }
  //                }
  //              }
  //            }
  //
  //            case PLATEAU_RAMP_PLATEAU -> {
  //              double t = Simulation.TIME;
  //
  //              double p1End = PlateauRampPlateauConstants.P1_TICKS;
  //              double rampEnd = p1End + PlateauRampPlateauConstants.RAMP_TICKS;
  //
  //              if (t < p1End) {
  //                // --------- PHASE 1: plateau constante ----------
  //                if (t % PlateauRampPlateauConstants.P1_INJECT_EVERY_N_TICKS == 0) {
  //                  int toInject =
  //                      Math.min(PlateauRampPlateauConstants.P1_BATCH_SIZE, packets.size());
  //                  for (int i = 0; i < toInject; i++) {
  //                    Packet p = packets.remove(0);
  //                    log.info(
  //                        "[time={}]: Injecting packet {} into the network at Node {}",
  //                        Simulation.TIME,
  //                        p.getId(),
  //                        p.getOrigin());
  //                    p.markAsDeparted();
  //                    network.getNode(p.getOrigin()).receivePacket(p);
  //                  }
  //                }
  //
  //              } else if (t < rampEnd) {
  //                // --------- PHASE 2: rampa lineal ----------
  //                // inicializá currentBatchSize al entrar en la rampa (por si venís de otro modo
  // /
  //                // run)
  //                if (t == p1End) {
  //                  currentBatchSize = PlateauRampPlateauConstants.RAMP_START_BATCH_SIZE;
  //                }
  //
  //                // subí la carga +1 cada RAMP_INCREASE_EVERY_N_TICKS
  //                if (((t - p1End) % PlateauRampPlateauConstants.RAMP_INCREASE_EVERY_N_TICKS) ==
  // 0) {
  //                  if (currentBatchSize < PlateauRampPlateauConstants.RAMP_MAX_BATCH_SIZE) {
  //                    currentBatchSize++;
  //                  }
  //                }
  //
  //                if (t % PlateauRampPlateauConstants.RAMP_INJECT_EVERY_N_TICKS == 0) {
  //                  int toInject = Math.min(currentBatchSize, packets.size());
  //                  for (int i = 0; i < toInject; i++) {
  //                    Packet p = packets.remove(0);
  //                    log.info(
  //                        "[time={}]: Injecting packet {} into the network at Node {}",
  //                        Simulation.TIME,
  //                        p.getId(),
  //                        p.getOrigin());
  //                    p.markAsDeparted();
  //                    network.getNode(p.getOrigin()).receivePacket(p);
  //                  }
  //                }
  //
  //              } else {
  //                // --------- PHASE 3: plateau alto ----------
  //                if (t % PlateauRampPlateauConstants.P3_INJECT_EVERY_N_TICKS == 0) {
  //                  int toInject =
  //                      Math.min(PlateauRampPlateauConstants.P3_BATCH_SIZE, packets.size());
  //                  for (int i = 0; i < toInject; i++) {
  //                    Packet p = packets.remove(0);
  //                    log.info(
  //                        "[time={}]: Injecting packet {} into the network at Node {}",
  //                        Simulation.TIME,
  //                        p.getId(),
  //                        p.getOrigin());
  //                    p.markAsDeparted();
  //                    network.getNode(p.getOrigin()).receivePacket(p);
  //                  }
  //                }
  //              }
  //            }
  //
  //            case FIXED_LOAD_STEPS -> {
  //              // en qué escalón estás
  //              int stepIndex = (int) (Simulation.TIME / FixedLoadStepConstants.STEP_TICKS);
  //
  //              // clamp al último valor si te pasás
  //              int batchSize =
  //                  FixedLoadStepConstants.BATCH_SIZES[
  //                      Math.min(stepIndex, FixedLoadStepConstants.BATCH_SIZES.length - 1)];
  //
  //              // inyección
  //              if (Simulation.TIME % FixedLoadStepConstants.INJECT_EVERY_N_TICKS == 0) {
  //
  //                int toInject = Math.min(batchSize, packets.size());
  //                for (int i = 0; i < toInject; i++) {
  //                  Packet p = packets.remove(0);
  //                  log.info(
  //                      "[time={}]: (fixed-load-steps step={} batch={}) Injecting packet {} at
  // Node {}",
  //                      Simulation.TIME,
  //                      stepIndex,
  //                      batchSize,
  //                      p.getId(),
  //                      p.getOrigin());
  //                  p.markAsDeparted();
  //                  network.getNode(p.getOrigin()).receivePacket(p);
  //                }
  //              }
  //            }
  //
  //            case WINDOWED_LOAD -> {
  //              if (Simulation.TIME % config.packetInjectGap() == 0 && Simulation.TIME < 200) {
  //                //                      if (Simulation.TIME % config.packetInjectGap() == 0 ||
  //                // Simulation.TIME > 200) {
  //                Packet packetToInject = packets.remove(0);
  //                log.info(
  //                    "[time={}]: Injecting packet {} into the network at Node {}",
  //                    Simulation.TIME,
  //                    packetToInject.getId(),
  //                    packetToInject.getOrigin());
  //                packetToInject.markAsDeparted();
  //                network.getNode(packetToInject.getOrigin()).receivePacket(packetToInject);
  //              }
  //
  //              if (Simulation.TIME > 200 && Simulation.TIME < 400) {
  //                for (int i = 0; i < 2; i++) {
  //                  Packet p = packets.remove(0);
  //                  log.info(
  //                      "[time={}]: Injecting packet {} into the network at Node {}",
  //                      Simulation.TIME,
  //                      p.getId(),
  //                      p.getOrigin());
  //                  p.markAsDeparted();
  //                  network.getNode(p.getOrigin()).receivePacket(p);
  //                }
  //              }
  //
  //              if (Simulation.TIME > 400) {
  //                Packet p = packets.remove(0);
  //                log.info(
  //                    "[time={}]: Injecting packet {} into the network at Node {}",
  //                    Simulation.TIME,
  //                    p.getId(),
  //                    p.getOrigin());
  //                p.markAsDeparted();
  //                network.getNode(p.getOrigin()).receivePacket(p);
  //              }
  //
  //              //                  int batchSize;
  //              //
  //              //                  // Phase A: [0, 200)
  //              //                  if (Simulation.TIME < WindowedLoadConstants.PHASE_A_TICKS) {
  //              //                      batchSize = WindowedLoadConstants.PHASE_A_BATCH;
  //              //
  //              //                      // Phase B: [200, 1000)
  //              //                  } else if (Simulation.TIME <
  // WindowedLoadConstants.PHASE_A_TICKS +
  //              // WindowedLoadConstants.PHASE_B_TICKS) {
  //              //                      batchSize = WindowedLoadConstants.PHASE_B_BATCH;
  //              //
  //              //                      // Phase C: [1000, ...)
  //              //                  } else {
  //              //                      batchSize = WindowedLoadConstants.PHASE_C_BATCH;
  //              //                  }
  //              //
  //              //                  int toInject = Math.min(batchSize, packets.size());
  //              //
  //              //                  if (toInject > 0) {
  //              //                      log.info(
  //              //                          "[time={}]: (WINDOWED_LOAD) Injecting {} packets at
  // Node
  //              // {} (phaseBatch={})",
  //              //                          Simulation.TIME,
  //              //                          toInject,
  //              //                          origin,
  //              //                          batchSize);
  //              //
  //              //                      for (int i = 0; i < toInject; i++) {
  //              //                          Packet p = packets.remove(0);
  //              //                          log.info(
  //              //                              "[time={}]: Injecting packet {} into the network
  // at
  //              // Node {}",
  //              //                              Simulation.TIME,
  //              //                              p.getId(),
  //              //                              origin);
  //              //                          p.markAsDeparted();
  //              //                          network.getNode(origin).receivePacket(p);
  //              //                      }
  //              //                  }
  //            }
  //          }
  //          tick();
  //        } else {
  //          tick();
  //        }
  //      }
  //      registry.plotAlgorithmSpecific(config);
  //    }
  //    registry.plotEverything(config);
  //  }

  private List<Packet> generatePacketsRandomly(List<Node> nodes, int totalPackets) {
    List<Node.Id> nodeIds = nodes.stream().map(Node::getId).toList();

    return new ArrayList<>(
        IntStream.range(0, totalPackets)
            .mapToObj(
                i -> {
                  Node.Id origin;
                  Node.Id destination;

                  do {
                    origin = nodeIds.get(RANDOM.nextIndex(nodeIds.size()));
                    destination = nodeIds.get(RANDOM.nextIndex(nodeIds.size()));
                  } while (origin.equals(destination));

                  return new Packet(new Packet.Id(i), origin, destination);
                })
            .toList());
  }

  private List<Packet> generatePacketsLeftToRight(List<Node> nodes, int totalPackets) {
    Set<Node.Id> LEFT_SUBSET =
        Set.of(
            new Node.Id(0),
            new Node.Id(1),
            new Node.Id(2),
            new Node.Id(6),
            new Node.Id(7),
            new Node.Id(8),
            new Node.Id(12),
            new Node.Id(13),
            new Node.Id(14),
            new Node.Id(18),
            new Node.Id(19),
            new Node.Id(20),
            new Node.Id(24),
            new Node.Id(25),
            new Node.Id(26),
            new Node.Id(30),
            new Node.Id(31),
            new Node.Id(32));

    //        Set<Node.Id> RIGHT_SUBSET =
    //            Set.of(
    //                new Node.Id(3), new Node.Id(4), new Node.Id(5),
    //                new Node.Id(9), new Node.Id(10), new Node.Id(11),
    //                new Node.Id(15), new Node.Id(16), new Node.Id(17),
    //                new Node.Id(21), new Node.Id(22), new Node.Id(23),
    //                new Node.Id(27), new Node.Id(28), new Node.Id(29),
    //                new Node.Id(33), new Node.Id(34), new Node.Id(35));

    Set<Node.Id> RIGHT_SUBSET = Set.of(new Node.Id(35));

    List<Node.Id> left = nodes.stream().map(Node::getId).filter(LEFT_SUBSET::contains).toList();
    List<Node.Id> right = nodes.stream().map(Node::getId).filter(RIGHT_SUBSET::contains).toList();

    if (left.isEmpty() || right.isEmpty()) {
      throw new IllegalStateException("Left/right subset is empty for this topology");
    }

    return new ArrayList<>(
        IntStream.range(0, totalPackets)
            .mapToObj(
                i -> {
                  Node.Id origin = left.get(RANDOM.nextIndex(left.size()));
                  Node.Id destination = right.get(RANDOM.nextIndex(right.size()));
                  return new Packet(new Packet.Id(i), origin, destination);
                })
            .toList());
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
    PLATEAU_THEN_LINEAR,
    PLATEAU_RAMP_PLATEAU,
    FIXED_LOAD_STEPS,
    WINDOWED_LOAD;

    public static TrafficInjectionMode getByConfig(SimulationConfig config) {
      if (config.linearIncrementalPacketInjection()) {
        return TrafficInjectionMode.LINEAR_INCREMENTAL;
      }
      if (config.plateauThenLinearPacketInjection()) {
        return TrafficInjectionMode.PLATEAU_THEN_LINEAR;
      }
      if (config.plateauRampPlateauPacketInjection()) {
        return TrafficInjectionMode.PLATEAU_RAMP_PLATEAU;
      }
      if (config.fixedLoadStepPacketInjection()) {
        return TrafficInjectionMode.FIXED_LOAD_STEPS;
      }
      if (config.windowedLoadPacketInjection()) {
        return TrafficInjectionMode.WINDOWED_LOAD;
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
