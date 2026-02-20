package org.ungs.core.traffic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Network;
import org.ungs.core.network.Node;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.core.traffic.pairs.NodePair;
import org.ungs.core.traffic.pairs.PairConstraint;
import org.ungs.core.traffic.pairs.PairSelector;
import org.ungs.core.traffic.runtime.TrafficInjector;
import org.ungs.core.traffic.schedule.InjectionSchedule;
import org.ungs.testutil.MockEventSink;
import org.ungs.testutil.TestConfigBuilder;
import org.ungs.testutil.TestNetworkBuilder;

@DisplayName("TrafficInjector")
class TrafficInjectorTest {

  private Network network;
  private MockEventSink eventSink;
  private SimulationRuntimeContext ctx;

  @BeforeEach
  void setUp() {
    network = TestNetworkBuilder.linearChain(5);
    eventSink = new MockEventSink();
    ctx = new SimulationRuntimeContext(TestConfigBuilder.withSeed(42L), network, eventSink);
    ctx.reset(AlgorithmType.Q_ROUTING);
    network.setRuntimeContext(ctx);
  }

  @Nested
  @DisplayName("Basic Injection")
  class BasicInjection {

    @Test
    @DisplayName("should inject packets according to schedule")
    void inject_followsSchedule() {
      InjectionSchedule schedule = c -> 3; // Always inject 3 packets
      PairSelector selector = c -> new NodePair(new Node.Id(0), new Node.Id(4));
      List<PairConstraint> constraints = List.of();
      int maxActive = 100;

      TrafficInjector injector = new TrafficInjector(schedule, selector, constraints, maxActive);

      assertEquals(0, network.packetsInFlight());

      injector.inject(ctx);

      assertEquals(3, network.packetsInFlight());
    }

    @Test
    @DisplayName("should inject zero packets when schedule returns 0")
    void inject_zeroFromSchedule() {
      InjectionSchedule schedule = c -> 0;
      PairSelector selector = c -> new NodePair(new Node.Id(0), new Node.Id(4));
      List<PairConstraint> constraints = List.of();
      int maxActive = 100;

      TrafficInjector injector = new TrafficInjector(schedule, selector, constraints, maxActive);

      injector.inject(ctx);

      assertEquals(0, network.packetsInFlight());
    }

    @Test
    @DisplayName("should inject packets at correct origin nodes")
    void inject_atCorrectOrigin() {
      InjectionSchedule schedule = c -> 1;
      PairSelector selector = c -> new NodePair(new Node.Id(2), new Node.Id(4));
      List<PairConstraint> constraints = List.of();
      int maxActive = 100;

      TrafficInjector injector = new TrafficInjector(schedule, selector, constraints, maxActive);

      injector.inject(ctx);

      Node node2 = network.getNode(new Node.Id(2));
      assertEquals(1, node2.getQueue().size());
      assertNotNull(node2.getQueue().peek());
      assertEquals(2, node2.getQueue().peek().getOrigin().value());
      assertNotNull(node2.getQueue().peek());
      assertEquals(4, node2.getQueue().peek().getDestination().value());
    }
  }

  @Nested
  @DisplayName("Backpressure Limiting")
  class BackpressureLimiting {

    @Test
    @DisplayName("should not inject when maxActivePackets reached")
    void inject_stopsAtMaxActive() {
      InjectionSchedule schedule = c -> 10;
      PairSelector selector = c -> new NodePair(new Node.Id(0), new Node.Id(4));
      List<PairConstraint> constraints = List.of();
      int maxActive = 5;

      TrafficInjector injector = new TrafficInjector(schedule, selector, constraints, maxActive);

      injector.inject(ctx);

      // Should only inject up to maxActive
      assertEquals(5, network.packetsInFlight());
    }

    @Test
    @DisplayName("should respect remaining slots when some packets exist")
    void inject_respectsRemainingSlots() {
      // Pre-populate some packets
      network
          .getNode(new Node.Id(0))
          .receivePacket(
              new org.ungs.core.network.Packet(
                  new org.ungs.core.network.Packet.Id(100), new Node.Id(0), new Node.Id(4)));
      network
          .getNode(new Node.Id(0))
          .receivePacket(
              new org.ungs.core.network.Packet(
                  new org.ungs.core.network.Packet.Id(101), new Node.Id(0), new Node.Id(4)));

      assertEquals(2, network.packetsInFlight());

      InjectionSchedule schedule = c -> 10;
      PairSelector selector = c -> new NodePair(new Node.Id(1), new Node.Id(4));
      List<PairConstraint> constraints = List.of();
      int maxActive = 5;

      TrafficInjector injector = new TrafficInjector(schedule, selector, constraints, maxActive);

      injector.inject(ctx);

      // Should only inject 3 more (5 - 2 = 3)
      assertEquals(5, network.packetsInFlight());
    }

    @Test
    @DisplayName("should not inject when no slots available")
    void inject_noSlotsAvailable() {
      // Fill up to max
      for (int i = 0; i < 5; i++) {
        network
            .getNode(new Node.Id(0))
            .receivePacket(
                new org.ungs.core.network.Packet(
                    new org.ungs.core.network.Packet.Id(100 + i), new Node.Id(0), new Node.Id(4)));
      }

      assertEquals(5, network.packetsInFlight());

      InjectionSchedule schedule = c -> 10;
      PairSelector selector = c -> new NodePair(new Node.Id(1), new Node.Id(4));
      List<PairConstraint> constraints = List.of();
      int maxActive = 5;

      TrafficInjector injector = new TrafficInjector(schedule, selector, constraints, maxActive);

      injector.inject(ctx);

      // No new packets should be injected
      assertEquals(5, network.packetsInFlight());
    }
  }

  @Nested
  @DisplayName("Pair Constraint Enforcement")
  class PairConstraintEnforcement {

    @Test
    @DisplayName("should skip injection when constraint rejects pair")
    void inject_skipsRejectedPairs() {
      InjectionSchedule schedule = c -> 5;
      PairSelector selector = c -> new NodePair(new Node.Id(0), new Node.Id(4));
      // Constraint that rejects all pairs
      PairConstraint rejectAll = (ctx, pair) -> false;
      List<PairConstraint> constraints = List.of(rejectAll);
      int maxActive = 100;

      TrafficInjector injector = new TrafficInjector(schedule, selector, constraints, maxActive);

      injector.inject(ctx);

      // No packets should be injected due to constraint
      assertEquals(0, network.packetsInFlight());
    }

    @Test
    @DisplayName("should inject when all constraints pass")
    void inject_withPassingConstraints() {
      InjectionSchedule schedule = c -> 3;
      PairSelector selector = c -> new NodePair(new Node.Id(0), new Node.Id(4));
      // Constraint that accepts all pairs
      PairConstraint acceptAll = (ctx, pair) -> true;
      List<PairConstraint> constraints = List.of(acceptAll);
      int maxActive = 100;

      TrafficInjector injector = new TrafficInjector(schedule, selector, constraints, maxActive);

      injector.inject(ctx);

      assertEquals(3, network.packetsInFlight());
    }

    @Test
    @DisplayName("should check all constraints in order")
    void inject_checksAllConstraints() {
      int[] checkCount = {0};
      InjectionSchedule schedule = c -> 1;
      PairSelector selector = c -> new NodePair(new Node.Id(0), new Node.Id(4));

      PairConstraint constraint1 =
          (ctx, pair) -> {
            checkCount[0]++;
            return true;
          };
      PairConstraint constraint2 =
          (ctx, pair) -> {
            checkCount[0]++;
            return true;
          };

      List<PairConstraint> constraints = List.of(constraint1, constraint2);
      int maxActive = 100;

      TrafficInjector injector = new TrafficInjector(schedule, selector, constraints, maxActive);

      injector.inject(ctx);

      // Both constraints should be checked
      assertEquals(2, checkCount[0]);
    }

    @Test
    @DisplayName("should short-circuit on first failing constraint")
    void inject_shortCircuitsOnFailure() {
      int[] checkCount = {0};
      InjectionSchedule schedule = c -> 1;
      PairSelector selector = c -> new NodePair(new Node.Id(0), new Node.Id(4));

      PairConstraint failingConstraint =
          (ctx, pair) -> {
            checkCount[0]++;
            return false;
          };
      PairConstraint neverChecked =
          (ctx, pair) -> {
            checkCount[0]++;
            return true;
          };

      List<PairConstraint> constraints = List.of(failingConstraint, neverChecked);
      int maxActive = 100;

      TrafficInjector injector = new TrafficInjector(schedule, selector, constraints, maxActive);

      injector.inject(ctx);

      // Only first constraint should be checked
      assertEquals(1, checkCount[0]);
    }
  }

  @Nested
  @DisplayName("Packet Creation")
  class PacketCreation {

    @Test
    @DisplayName("should create packets with unique IDs")
    void inject_uniquePacketIds() {
      InjectionSchedule schedule = c -> 5;
      PairSelector selector = c -> new NodePair(new Node.Id(0), new Node.Id(4));
      List<PairConstraint> constraints = List.of();
      int maxActive = 100;

      TrafficInjector injector = new TrafficInjector(schedule, selector, constraints, maxActive);

      injector.inject(ctx);

      // Collect all packet IDs
      var packets = network.getNode(new Node.Id(0)).getQueue();
      long uniqueCount = packets.stream().map(p -> p.getId().value()).distinct().count();

      assertEquals(5, uniqueCount);
    }

    @Test
    @DisplayName("should mark packets as departed on creation")
    void inject_marksPacketsAsDeparted() {
      InjectionSchedule schedule = c -> 1;
      PairSelector selector = c -> new NodePair(new Node.Id(0), new Node.Id(4));
      List<PairConstraint> constraints = List.of();
      int maxActive = 100;

      TrafficInjector injector = new TrafficInjector(schedule, selector, constraints, maxActive);

      injector.inject(ctx);

      var packet = network.getNode(new Node.Id(0)).getQueue().peek();
      assertNotNull(packet);
      assertTrue(packet.getDepartureTime() >= 0, "Packet should have departure time set");
    }

    @Test
    @DisplayName("should emit PacketDepartedEvent on creation")
    void inject_emitsDepartedEvent() {
      InjectionSchedule schedule = c -> 1;
      PairSelector selector = c -> new NodePair(new Node.Id(0), new Node.Id(4));
      List<PairConstraint> constraints = List.of();
      int maxActive = 100;

      TrafficInjector injector = new TrafficInjector(schedule, selector, constraints, maxActive);

      injector.inject(ctx);

      assertEquals(1, eventSink.getDepartedEvents().size());
    }
  }

  @Nested
  @DisplayName("Pair Selector Usage")
  class PairSelectorUsage {

    @Test
    @DisplayName("should call pair selector for each injection")
    void inject_callsSelectorPerPacket() {
      int[] callCount = {0};
      InjectionSchedule schedule = c -> 5;
      PairSelector selector =
          c -> {
            callCount[0]++;
            return new NodePair(new Node.Id(0), new Node.Id(4));
          };
      List<PairConstraint> constraints = List.of();
      int maxActive = 100;

      TrafficInjector injector = new TrafficInjector(schedule, selector, constraints, maxActive);

      injector.inject(ctx);

      assertEquals(5, callCount[0]);
    }

    @Test
    @DisplayName("should use different pairs from selector")
    void inject_usesDifferentPairs() {
      int[] nextOrigin = {0};
      InjectionSchedule schedule = c -> 3;
      PairSelector selector =
          c -> {
            int origin = nextOrigin[0]++;
            return new NodePair(new Node.Id(origin), new Node.Id(4));
          };
      List<PairConstraint> constraints = List.of();
      int maxActive = 100;

      TrafficInjector injector = new TrafficInjector(schedule, selector, constraints, maxActive);

      injector.inject(ctx);

      // Packets should be at nodes 0, 1, 2
      assertEquals(1, network.getNode(new Node.Id(0)).getQueue().size());
      assertEquals(1, network.getNode(new Node.Id(1)).getQueue().size());
      assertEquals(1, network.getNode(new Node.Id(2)).getQueue().size());
    }
  }

  @Nested
  @DisplayName("Multiple Injection Calls")
  class MultipleInjectionCalls {

    @Test
    @DisplayName("should inject across multiple ticks")
    void inject_multipleTicksAccumulate() {
      InjectionSchedule schedule = c -> 2;
      PairSelector selector = c -> new NodePair(new Node.Id(0), new Node.Id(4));
      List<PairConstraint> constraints = List.of();
      int maxActive = 100;

      TrafficInjector injector = new TrafficInjector(schedule, selector, constraints, maxActive);

      injector.inject(ctx);
      ctx.advanceOneTick();
      injector.inject(ctx);
      ctx.advanceOneTick();
      injector.inject(ctx);

      assertEquals(6, network.packetsInFlight());
    }
  }
}
