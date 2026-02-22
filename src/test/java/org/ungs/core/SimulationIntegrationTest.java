package org.ungs.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ungs.core.config.*;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Network;
import org.ungs.core.network.Node;
import org.ungs.core.network.Packet;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.core.routing.impl.qrouting.QRoutingApplication;
import org.ungs.core.routing.impl.shortestpath.ShortestPathApplication;
import org.ungs.core.topology.api.TopologyType;
import org.ungs.testutil.MockEventSink;
import org.ungs.testutil.TestNetworkBuilder;

@DisplayName("Simulation Integration Tests")
class SimulationIntegrationTest {

  @Nested
  @DisplayName("Mini Simulation - 3-5 Node Networks")
  class MiniSimulation {

    @Test
    @DisplayName("should deliver packets in 3-node linear chain")
    void threeNodeChain_deliversPackets() {
      // Setup: 3 nodes in chain 0-1-2
      Network network = TestNetworkBuilder.linearChain(3);
      SimulationConfigContext config = createConfig(50, 1.0, 42L);
      MockEventSink eventSink = new MockEventSink();

      SimulationRuntimeContext ctx = new SimulationRuntimeContext(config, network, eventSink);
      ctx.reset(AlgorithmType.Q_ROUTING);
      network.setRuntimeContext(ctx);

      // Install routing apps
      for (Node node : network.getNodes()) {
        node.installApplication(new QRoutingApplication(node, ctx));
      }

      // Inject a packet from 0 to 2
      Packet packet = new Packet(ctx.nextPacketId(), new Node.Id(0), new Node.Id(2));
      packet.markAsDeparted(ctx);
      network.getNode(new Node.Id(0)).receivePacket(packet);

      // Simulate ticks
      for (int tick = 0; tick < 20; tick++) {
        for (Node node : network.getNodes()) {
          node.getApplication().onTick(ctx);
        }

        // Flush pending sends
        var sends = ctx.flushPendingSends();
        for (var send : sends) {
          network.sendPacket(send.from(), send.to(), send.packet());
        }

        // Check for delivery
        if (!eventSink.getDeliveredEvents().isEmpty()) {
          break;
        }

        ctx.advanceOneTick();
      }

      // Verify packet was delivered
      assertEquals(1, eventSink.getDeliveredEvents().size());
      assertEquals(packet, eventSink.getDeliveredEvents().getFirst().packet());
    }

    @Test
    @DisplayName("should deliver packets in 5-node star topology")
    void fiveNodeStar_deliversPackets() {
      Network network = TestNetworkBuilder.star(4); // hub + 4 spokes
      SimulationConfigContext config = createConfig(50, 1.0, 42L);
      MockEventSink eventSink = new MockEventSink();

      SimulationRuntimeContext ctx = new SimulationRuntimeContext(config, network, eventSink);
      ctx.reset(AlgorithmType.SHORTEST_PATH);
      network.setRuntimeContext(ctx);

      for (Node node : network.getNodes()) {
        node.installApplication(new ShortestPathApplication(node));
      }

      // Inject packet from spoke 1 to spoke 4 (must go through hub 0)
      Packet packet = new Packet(ctx.nextPacketId(), new Node.Id(1), new Node.Id(4));
      packet.markAsDeparted(ctx);
      network.getNode(new Node.Id(1)).receivePacket(packet);

      for (int tick = 0; tick < 20; tick++) {
        for (Node node : network.getNodes()) {
          node.getApplication().onTick(ctx);
        }

        var sends = ctx.flushPendingSends();
        for (var send : sends) {
          network.sendPacket(send.from(), send.to(), send.packet());
        }

        if (!eventSink.getDeliveredEvents().isEmpty()) {
          break;
        }

        ctx.advanceOneTick();
      }

      assertEquals(1, eventSink.getDeliveredEvents().size());
    }
  }

  @Nested
  @DisplayName("Algorithm Comparison")
  class AlgorithmComparison {

    @Test
    @DisplayName("should run same traffic pattern with Q-Routing and Shortest-Path")
    void sameTrafficBothAlgorithms() {
      // Test that both algorithms can route the same packet successfully
      Network network1 = TestNetworkBuilder.linearChain(5);
      Network network2 = TestNetworkBuilder.linearChain(5);

      MockEventSink sink1 = new MockEventSink();
      MockEventSink sink2 = new MockEventSink();

      // Q-Routing
      runSinglePacketSimulation(network1, sink1, AlgorithmType.Q_ROUTING, 42L);

      // Shortest-Path
      runSinglePacketSimulation(network2, sink2, AlgorithmType.SHORTEST_PATH, 42L);

      // Both should deliver the packet
      assertFalse(sink1.getDeliveredEvents().isEmpty(), "Q-Routing should deliver packet");
      assertFalse(sink2.getDeliveredEvents().isEmpty(), "Shortest-Path should deliver packet");
    }

    private void runSinglePacketSimulation(
        Network network, MockEventSink eventSink, AlgorithmType algorithm, long seed) {
      SimulationConfigContext config = createConfig(100, 0, seed);
      SimulationRuntimeContext ctx = new SimulationRuntimeContext(config, network, eventSink);
      ctx.reset(algorithm);
      network.setRuntimeContext(ctx);

      for (Node node : network.getNodes()) {
        if (algorithm == AlgorithmType.Q_ROUTING) {
          node.installApplication(new QRoutingApplication(node, ctx));
        } else {
          node.installApplication(new ShortestPathApplication(node));
        }
      }

      Packet packet = new Packet(ctx.nextPacketId(), new Node.Id(0), new Node.Id(4));
      packet.markAsDeparted(ctx);
      network.getNode(new Node.Id(0)).receivePacket(packet);

      for (int tick = 0; tick < 50; tick++) {
        for (Node node : network.getNodes()) {
          node.getApplication().onTick(ctx);
        }

        var sends = ctx.flushPendingSends();
        for (var send : sends) {
          network.sendPacket(send.from(), send.to(), send.packet());
        }

        if (!eventSink.getDeliveredEvents().isEmpty()) {
          return;
        }

        ctx.advanceOneTick();
      }
    }
  }

  @Nested
  @DisplayName("Determinism Verification")
  class DeterminismVerification {

    @Test
    @DisplayName("same config and seed should produce identical final states")
    void sameConfigAndSeed_identicalResults() {
      long seed = 12345L;

      // Run 1
      Network network1 = TestNetworkBuilder.grid(3, 3);
      MockEventSink sink1 = new MockEventSink();
      int deliveries1 = runSimulation(network1, sink1, seed);

      // Run 2 with same seed
      Network network2 = TestNetworkBuilder.grid(3, 3);
      MockEventSink sink2 = new MockEventSink();
      int deliveries2 = runSimulation(network2, sink2, seed);

      // Results should be identical
      assertEquals(deliveries1, deliveries2, "Same seed should produce identical delivery counts");
      assertEquals(
          sink1.getHopEvents().size(),
          sink2.getHopEvents().size(),
          "Same seed should produce identical hop counts");
    }

    private int runSimulation(Network network, MockEventSink eventSink, long seed) {
      SimulationConfigContext config = createConfig(30, 1.0, seed);
      SimulationRuntimeContext ctx = new SimulationRuntimeContext(config, network, eventSink);
      ctx.reset(AlgorithmType.Q_ROUTING);
      network.setRuntimeContext(ctx);

      for (Node node : network.getNodes()) {
        node.installApplication(new QRoutingApplication(node, ctx));
      }

      for (int tick = 0; tick < 30; tick++) {
        // Inject packets based on load level
        if (ctx.getRng().nextUnitDouble() < 0.5) {
          List<Node> nodes = network.getNodes();
          Node origin = nodes.get(ctx.getRng().nextIndex(nodes.size()));
          Node dest;
          do {
            dest = nodes.get(ctx.getRng().nextIndex(nodes.size()));
          } while (dest.equals(origin));

          Packet packet = new Packet(ctx.nextPacketId(), origin.getId(), dest.getId());
          packet.markAsDeparted(ctx);
          origin.receivePacket(packet);
        }

        for (Node node : network.getNodes()) {
          node.getApplication().onTick(ctx);
        }

        var sends = ctx.flushPendingSends();
        for (var send : sends) {
          network.sendPacket(send.from(), send.to(), send.packet());
        }

        ctx.advanceOneTick();
      }

      return eventSink.getDeliveredEvents().size();
    }
  }

  @Nested
  @DisplayName("Property-Based Tests")
  class PropertyBasedTests {

    @Test
    @DisplayName("delivered packets count should never exceed injected packets")
    void deliveredNeverExceedsInjected() {
      Network network = TestNetworkBuilder.grid(4, 4);
      MockEventSink eventSink = new MockEventSink();
      SimulationConfigContext config = createConfig(100, 2.0, 42L);
      SimulationRuntimeContext ctx = new SimulationRuntimeContext(config, network, eventSink);
      ctx.reset(AlgorithmType.Q_ROUTING);
      network.setRuntimeContext(ctx);

      for (Node node : network.getNodes()) {
        node.installApplication(new QRoutingApplication(node, ctx));
      }

      int injectedCount = 0;

      for (int tick = 0; tick < 100; tick++) {
        // Inject packets
        int toInject = (int) Math.floor(2.0);
        for (int i = 0; i < toInject; i++) {
          List<Node> nodes = network.getNodes();
          Node origin = nodes.get(ctx.getRng().nextIndex(nodes.size()));
          Node dest;
          do {
            dest = nodes.get(ctx.getRng().nextIndex(nodes.size()));
          } while (dest.equals(origin));

          Packet packet = new Packet(ctx.nextPacketId(), origin.getId(), dest.getId());
          packet.markAsDeparted(ctx);
          origin.receivePacket(packet);
          injectedCount++;
        }

        for (Node node : network.getNodes()) {
          node.getApplication().onTick(ctx);
        }

        var sends = ctx.flushPendingSends();
        for (var send : sends) {
          network.sendPacket(send.from(), send.to(), send.packet());
        }

        ctx.advanceOneTick();

        // Property: delivered <= injected
        assertTrue(
            eventSink.getDeliveredEvents().size() <= injectedCount,
            "Delivered count should never exceed injected count");
      }
    }

    @Test
    @DisplayName("shortest-path should always decrease distance to destination when path exists")
    void shortestPath_alwaysDecreasesDistance() {
      Network network = TestNetworkBuilder.linearChain(10);
      MockEventSink eventSink = new MockEventSink();
      SimulationConfigContext config = createConfig(50, 0, 42L);
      SimulationRuntimeContext ctx = new SimulationRuntimeContext(config, network, eventSink);
      ctx.reset(AlgorithmType.SHORTEST_PATH);
      network.setRuntimeContext(ctx);

      for (Node node : network.getNodes()) {
        node.installApplication(new ShortestPathApplication(node));
      }

      // Inject packet from 0 to 9
      Packet packet = new Packet(ctx.nextPacketId(), new Node.Id(0), new Node.Id(9));
      packet.markAsDeparted(ctx);
      network.getNode(new Node.Id(0)).receivePacket(packet);

      int previousDistance = network.getDistanceTo(new Node.Id(0), new Node.Id(9));
      Node.Id currentLocation;

      for (int tick = 0; tick < 20; tick++) {
        for (Node node : network.getNodes()) {
          node.getApplication().onTick(ctx);
        }

        var sends = ctx.flushPendingSends();
        if (!sends.isEmpty()) {
          var send = sends.getFirst();
          currentLocation = send.to();

          int newDistance = network.getDistanceTo(currentLocation, new Node.Id(9));

          // Property: distance should decrease (or be 0 if at destination)
          assertTrue(
              newDistance < previousDistance || newDistance == 0,
              "Distance should decrease: " + previousDistance + " -> " + newDistance);

          previousDistance = newDistance;

          network.sendPacket(send.from(), send.to(), send.packet());
        }

        if (!eventSink.getDeliveredEvents().isEmpty()) {
          break;
        }

        ctx.advanceOneTick();
      }
    }
  }

  @Nested
  @DisplayName("Special Scenarios")
  class SpecialScenarios {

    @Test
    @DisplayName("should handle backpressure when max active packets reached")
    void backpressureScenario() {
      Network network = TestNetworkBuilder.linearChain(3);
      MockEventSink eventSink = new MockEventSink();
      SimulationConfigContext config = createConfigWithMaxActive(100, 5.0, 42L, 5);
      SimulationRuntimeContext ctx = new SimulationRuntimeContext(config, network, eventSink);
      ctx.reset(AlgorithmType.Q_ROUTING);
      network.setRuntimeContext(ctx);

      for (Node node : network.getNodes()) {
        node.installApplication(new QRoutingApplication(node, ctx));
      }

      // Try to inject many packets
      for (int i = 0; i < 100; i++) {
        Packet packet = new Packet(ctx.nextPacketId(), new Node.Id(0), new Node.Id(2));
        packet.markAsDeparted(ctx);
        network.getNode(new Node.Id(0)).receivePacket(packet);
      }

      // Network should have packets
      assertTrue(network.packetsInFlight() > 0);
    }

    @Test
    @DisplayName("should handle isolated nodes gracefully")
    void isolatedNodeScenario() {
      Network network = TestNetworkBuilder.disconnectedPair();
      MockEventSink eventSink = new MockEventSink();
      SimulationConfigContext config = createConfig(20, 0, 42L);
      SimulationRuntimeContext ctx = new SimulationRuntimeContext(config, network, eventSink);
      ctx.reset(AlgorithmType.Q_ROUTING);
      network.setRuntimeContext(ctx);

      for (Node node : network.getNodes()) {
        node.installApplication(new QRoutingApplication(node, ctx));
      }

      // Inject packet - it cannot be delivered
      Packet packet = new Packet(ctx.nextPacketId(), new Node.Id(0), new Node.Id(1));
      packet.markAsDeparted(ctx);
      network.getNode(new Node.Id(0)).receivePacket(packet);

      // Run simulation - should not throw
      for (int tick = 0; tick < 20; tick++) {
        for (Node node : network.getNodes()) {
          assertDoesNotThrow(() -> node.getApplication().onTick(ctx));
        }

        var sends = ctx.flushPendingSends();
        for (var send : sends) {
          network.sendPacket(send.from(), send.to(), send.packet());
        }

        ctx.advanceOneTick();
      }

      // Packet should not be delivered (nodes are disconnected)
      assertTrue(eventSink.getDeliveredEvents().isEmpty());
    }
  }

  // Helper methods

  private SimulationConfigContext createConfig(int ticks, double loadLevel, long seed) {
    GeneralConfig general =
        new GeneralConfig(
            seed,
            TopologyType._6X6_GRID,
            null,
            List.of(AlgorithmType.Q_ROUTING),
            OptionalInt.of(1000),
            0,
            "test",
            null);

    TerminationConfig termination = new TerminationConfig.FixedTicks(ticks);

    InjectionScheduleConfig schedule = new InjectionScheduleConfig.LoadLevel(loadLevel);
    PairSelectionConfig pairSelection = new PairSelectionConfig.Random();
    PairConstraintsConfig constraints = new PairConstraintsConfig(true, false);
    TrafficConfig traffic =
        new TrafficConfig(schedule, pairSelection, constraints, new GroupsConfig(Map.of()));

    NetworkDynamicsConfig dynamics = new NetworkDynamicsConfig.None();
    ObservabilityConfig observability =
        new ObservabilityConfig(List.of(), List.of(), 1, 500, 0, OptionalLong.empty());

    return new SimulationConfigContext(general, termination, traffic, dynamics, observability);
  }

  private SimulationConfigContext createConfigWithMaxActive(
      int ticks, double loadLevel, long seed, int maxActive) {
    GeneralConfig general =
        new GeneralConfig(
            seed,
            TopologyType._6X6_GRID,
            null,
            List.of(AlgorithmType.Q_ROUTING),
            OptionalInt.of(maxActive),
            0,
            "test",
            null);

    TerminationConfig termination = new TerminationConfig.FixedTicks(ticks);

    InjectionScheduleConfig schedule = new InjectionScheduleConfig.LoadLevel(loadLevel);
    PairSelectionConfig pairSelection = new PairSelectionConfig.Random();
    PairConstraintsConfig constraints = new PairConstraintsConfig(true, false);
    TrafficConfig traffic =
        new TrafficConfig(schedule, pairSelection, constraints, new GroupsConfig(Map.of()));

    NetworkDynamicsConfig dynamics = new NetworkDynamicsConfig.None();
    ObservabilityConfig observability =
        new ObservabilityConfig(List.of(), List.of(), 1, 500, 0, OptionalLong.empty());

    return new SimulationConfigContext(general, termination, traffic, dynamics, observability);
  }
}
