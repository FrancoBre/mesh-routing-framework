package org.ungs.core.traffic.pairs;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ungs.core.config.*;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Network;
import org.ungs.core.network.Node;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.core.traffic.runtime.TrafficBuildContext;
import org.ungs.testutil.MockEventSink;
import org.ungs.testutil.TestConfigBuilder;
import org.ungs.testutil.TestNetworkBuilder;

@DisplayName("Pair Selector Presets")
class PairSelectorPresetsTest {

  private Network network;
  private MockEventSink eventSink;

  @BeforeEach
  void setUp() {
    network = TestNetworkBuilder.linearChain(5);
    eventSink = new MockEventSink();
  }

  private SimulationRuntimeContext createContext(long seed) {
    SimulationRuntimeContext ctx =
        new SimulationRuntimeContext(TestConfigBuilder.withSeed(seed), network, eventSink);
    ctx.reset(AlgorithmType.Q_ROUTING);
    return ctx;
  }

  @Nested
  @DisplayName("RandomPairSelectorPreset")
  class RandomPairSelectorTests {

    @Test
    @DisplayName("should select origin from network nodes")
    void pickPair_originFromNetwork() {
      RandomPairSelectorPreset preset = new RandomPairSelectorPreset();
      TrafficBuildContext buildCtx = createTrafficBuildContext();
      PairSelector selector = preset.create(new PairSelectionConfig.Random(), buildCtx);

      SimulationRuntimeContext ctx = createContext(42L);
      NodePair pair = selector.pickPair(ctx);

      List<Integer> nodeIds = List.of(0, 1, 2, 3, 4);
      assertTrue(nodeIds.contains(pair.origin().value()));
    }

    @Test
    @DisplayName("should select destination from network nodes")
    void pickPair_destinationFromNetwork() {
      RandomPairSelectorPreset preset = new RandomPairSelectorPreset();
      TrafficBuildContext buildCtx = createTrafficBuildContext();
      PairSelector selector = preset.create(new PairSelectionConfig.Random(), buildCtx);

      SimulationRuntimeContext ctx = createContext(42L);
      NodePair pair = selector.pickPair(ctx);

      List<Integer> nodeIds = List.of(0, 1, 2, 3, 4);
      assertTrue(nodeIds.contains(pair.destination().value()));
    }

    @Test
    @DisplayName("should ensure origin != destination")
    void pickPair_originDifferentFromDestination() {
      RandomPairSelectorPreset preset = new RandomPairSelectorPreset();
      TrafficBuildContext buildCtx = createTrafficBuildContext();
      PairSelector selector = preset.create(new PairSelectionConfig.Random(), buildCtx);

      // Test many times to ensure we never get origin == destination
      for (int i = 0; i < 100; i++) {
        SimulationRuntimeContext ctx = createContext(i);
        NodePair pair = selector.pickPair(ctx);
        assertNotEquals(pair.origin(), pair.destination(), "Origin should not equal destination");
      }
    }

    @Test
    @DisplayName("should provide uniform distribution across nodes")
    void pickPair_uniformDistribution() {
      RandomPairSelectorPreset preset = new RandomPairSelectorPreset();
      TrafficBuildContext buildCtx = createTrafficBuildContext();
      PairSelector selector = preset.create(new PairSelectionConfig.Random(), buildCtx);

      int[] originCounts = new int[5];
      int trials = 1000;

      for (int i = 0; i < trials; i++) {
        SimulationRuntimeContext ctx = createContext(i);
        NodePair pair = selector.pickPair(ctx);
        originCounts[pair.origin().value()]++;
      }

      // Each node should be selected roughly 20% of the time (1/5)
      double expectedCount = trials / 5.0;
      for (int i = 0; i < 5; i++) {
        double deviation = Math.abs(originCounts[i] - expectedCount) / expectedCount;
        assertTrue(
            deviation < 0.3,
            "Node " + i + " selection count too far from expected: " + originCounts[i]);
      }
    }

    @Test
    @DisplayName("should return correct type")
    void type_returnsRandom() {
      RandomPairSelectorPreset preset = new RandomPairSelectorPreset();
      assertEquals(PairSelectionType.RANDOM, preset.type());
    }

    private TrafficBuildContext createTrafficBuildContext() {
      SimulationConfigContext config = TestConfigBuilder.minimal();
      List<Node.Id> stableNodeIds = network.getNodes().stream().map(Node::getId).toList();
      return new TrafficBuildContext(config, network, stableNodeIds);
    }
  }

  @Nested
  @DisplayName("OscillatingBetweenGroupsPairSelectorPreset")
  class OscillatingPairSelectorTests {

    private SimulationConfigContext configWithGroups;

    @BeforeEach
    void setUpGroups() {
      // Create config with group definitions
      // Group A: nodes 0, 1
      // Group B: nodes 3, 4
      configWithGroups = createConfigWithGroups();
    }

    @Test
    @DisplayName("should select from group A to group B in first period")
    void firstPeriod_selectsAToB() {
      OscillatingBetweenGroupsPairSelectorPreset preset =
          new OscillatingBetweenGroupsPairSelectorPreset();
      TrafficBuildContext buildCtx = createTrafficBuildContextWithGroups();
      PairSelectionConfig.OscillatingBetweenGroups config =
          new PairSelectionConfig.OscillatingBetweenGroups("groupA", "groupB", 10);
      PairSelector selector = preset.create(config, buildCtx);

      // At tick 0 (first period), should go from A to B
      SimulationRuntimeContext ctx =
          new SimulationRuntimeContext(configWithGroups, network, eventSink);
      ctx.reset(AlgorithmType.Q_ROUTING);

      NodePair pair = selector.pickPair(ctx);

      // Origin should be from group A (0 or 1)
      assertTrue(
          pair.origin().value() == 0 || pair.origin().value() == 1,
          "Origin should be from group A: " + pair.origin());
      // Destination should be from group B (3 or 4)
      assertTrue(
          pair.destination().value() == 3 || pair.destination().value() == 4,
          "Destination should be from group B: " + pair.destination());
    }

    @Test
    @DisplayName("should alternate direction after period ticks")
    void afterPeriod_alternatesDirection() {
      OscillatingBetweenGroupsPairSelectorPreset preset =
          new OscillatingBetweenGroupsPairSelectorPreset();
      TrafficBuildContext buildCtx = createTrafficBuildContextWithGroups();
      PairSelectionConfig.OscillatingBetweenGroups config =
          new PairSelectionConfig.OscillatingBetweenGroups("groupA", "groupB", 10);
      PairSelector selector = preset.create(config, buildCtx);

      SimulationRuntimeContext ctx =
          new SimulationRuntimeContext(configWithGroups, network, eventSink);
      ctx.reset(AlgorithmType.Q_ROUTING);

      // Advance past first period
      for (int i = 0; i < 10; i++) {
        ctx.advanceOneTick();
      }

      NodePair pair = selector.pickPair(ctx);

      // In second period, should go from B to A
      // Origin should be from group B (3 or 4)
      assertTrue(
          pair.origin().value() == 3 || pair.origin().value() == 4,
          "Origin should be from group B in second period: " + pair.origin());
      // Destination should be from group A (0 or 1)
      assertTrue(
          pair.destination().value() == 0 || pair.destination().value() == 1,
          "Destination should be from group A in second period: " + pair.destination());
    }

    @Test
    @DisplayName("should continue oscillating across multiple periods")
    void multiplePeriods_continuesOscillating() {
      OscillatingBetweenGroupsPairSelectorPreset preset =
          new OscillatingBetweenGroupsPairSelectorPreset();
      TrafficBuildContext buildCtx = createTrafficBuildContextWithGroups();
      PairSelectionConfig.OscillatingBetweenGroups config =
          new PairSelectionConfig.OscillatingBetweenGroups("groupA", "groupB", 5);
      PairSelector selector = preset.create(config, buildCtx);

      SimulationRuntimeContext ctx =
          new SimulationRuntimeContext(configWithGroups, network, eventSink);
      ctx.reset(AlgorithmType.Q_ROUTING);

      Set<Integer> groupA = Set.of(0, 1);
      Set<Integer> groupB = Set.of(3, 4);

      // Period 0 (ticks 0-4): A -> B
      NodePair pair0 = selector.pickPair(ctx);
      assertTrue(groupA.contains(pair0.origin().value()));
      assertTrue(groupB.contains(pair0.destination().value()));

      // Advance to period 1 (ticks 5-9): B -> A
      for (int i = 0; i < 5; i++) ctx.advanceOneTick();
      NodePair pair1 = selector.pickPair(ctx);
      assertTrue(groupB.contains(pair1.origin().value()));
      assertTrue(groupA.contains(pair1.destination().value()));

      // Advance to period 2 (ticks 10-14): A -> B again
      for (int i = 0; i < 5; i++) ctx.advanceOneTick();
      NodePair pair2 = selector.pickPair(ctx);
      assertTrue(groupA.contains(pair2.origin().value()));
      assertTrue(groupB.contains(pair2.destination().value()));
    }

    @Test
    @DisplayName("should return correct type")
    void type_returnsOscillating() {
      OscillatingBetweenGroupsPairSelectorPreset preset =
          new OscillatingBetweenGroupsPairSelectorPreset();
      assertEquals(PairSelectionType.OSCILLATING_BETWEEN_GROUPS, preset.type());
    }

    private SimulationConfigContext createConfigWithGroups() {
      Map<String, List<Integer>> groups =
          Map.of(
              "groupA", List.of(0, 1),
              "groupB", List.of(3, 4));

      GeneralConfig general =
          new GeneralConfig(
              42L,
              org.ungs.core.topology.api.TopologyType._6X6_GRID,
              null,
              List.of(AlgorithmType.Q_ROUTING),
              java.util.OptionalInt.of(100),
              0,
              "test",
              null);

      TerminationConfig termination = new TerminationConfig.FixedTicks(100);

      InjectionScheduleConfig schedule = new InjectionScheduleConfig.LoadLevel(1.0);
      PairSelectionConfig pairSelection =
          new PairSelectionConfig.OscillatingBetweenGroups("groupA", "groupB", 10);
      PairConstraintsConfig constraints = new PairConstraintsConfig(true, false);
      TrafficConfig traffic =
          new TrafficConfig(schedule, pairSelection, constraints, new GroupsConfig(groups));

      NetworkDynamicsConfig dynamics = new NetworkDynamicsConfig.None();
      ObservabilityConfig observability =
          new ObservabilityConfig(List.of(), List.of(), 1, 0, OptionalLong.empty());

      return new SimulationConfigContext(general, termination, traffic, dynamics, observability);
    }

    private TrafficBuildContext createTrafficBuildContextWithGroups() {
      List<Node.Id> stableNodeIds = network.getNodes().stream().map(Node::getId).toList();
      return new TrafficBuildContext(configWithGroups, network, stableNodeIds);
    }
  }
}
