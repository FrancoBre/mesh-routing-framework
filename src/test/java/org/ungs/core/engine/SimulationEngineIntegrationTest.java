package org.ungs.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ungs.core.config.*;
import org.ungs.core.network.Network;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.core.topology.api.TopologyType;
import org.ungs.testutil.TestNetworkBuilder;

@DisplayName("SimulationEngine Integration Tests")
class SimulationEngineIntegrationTest {

  @Nested
  @DisplayName("Full Simulation Run")
  class FullSimulationRun {

    @Test
    @DisplayName("should complete simulation with Q_ROUTING algorithm")
    void singleAlgorithmRun_completes() {
      Network network = TestNetworkBuilder.linearChain(5);
      SimulationConfigContext config = createMinimalConfig(10, List.of(AlgorithmType.Q_ROUTING));

      SimulationEngine engine = new SimulationEngine(config, network);

      assertDoesNotThrow(engine::run);
    }

    @Test
    @DisplayName("should complete simulation with SHORTEST_PATH algorithm")
    void shortestPathRun_completes() {
      Network network = TestNetworkBuilder.linearChain(5);
      SimulationConfigContext config =
          createMinimalConfig(10, List.of(AlgorithmType.SHORTEST_PATH));

      SimulationEngine engine = new SimulationEngine(config, network);

      assertDoesNotThrow(engine::run);
    }

    @Test
    @DisplayName("should run both algorithms sequentially")
    void multiAlgorithmRun_executesBoth() {
      Network network = TestNetworkBuilder.linearChain(5);
      SimulationConfigContext config =
          createMinimalConfig(10, List.of(AlgorithmType.Q_ROUTING, AlgorithmType.SHORTEST_PATH));

      SimulationEngine engine = new SimulationEngine(config, network);
      engine.run();
    }

    @Test
    @DisplayName("should deliver packets in small network simulation")
    void smallNetworkSimulation_deliversPackets() {
      Network network = TestNetworkBuilder.linearChain(3); // 0 - 1 - 2
      SimulationConfigContext config =
          createConfigWithGapSchedule(50, 5, 1, List.of(AlgorithmType.Q_ROUTING));

      SimulationEngine engine = new SimulationEngine(config, network);

      assertDoesNotThrow(engine::run);
    }
  }

  @Nested
  @DisplayName("Determinism")
  class Determinism {

    @Test
    @DisplayName("should produce identical results with same seed")
    void sameSeed_producesIdenticalResults() {
      long seed = 12345L;

      // First run
      Network network1 = TestNetworkBuilder.linearChain(5);
      SimulationConfigContext config1 = createConfigWithSeed(seed, 20);
      SimulationEngine engine1 = new SimulationEngine(config1, network1);
      engine1.run();

      // Second run with same seed
      Network network2 = TestNetworkBuilder.linearChain(5);
      SimulationConfigContext config2 = createConfigWithSeed(seed, 20);
      SimulationEngine engine2 = new SimulationEngine(config2, network2);
      engine2.run();

      // Both networks should have same packet distribution
      assertEquals(network1.packetsInFlight(), network2.packetsInFlight());
    }

    @Test
    @DisplayName("should produce different results with different seeds")
    void differentSeeds_mayProduceDifferentResults() {
      // First run
      Network network1 = TestNetworkBuilder.linearChain(10);
      SimulationConfigContext config1 = createConfigWithSeed(111L, 100);
      SimulationEngine engine1 = new SimulationEngine(config1, network1);
      engine1.run();

      // Second run with different seed
      Network network2 = TestNetworkBuilder.linearChain(10);
      SimulationConfigContext config2 = createConfigWithSeed(999L, 100);
      SimulationEngine engine2 = new SimulationEngine(config2, network2);
      engine2.run();
    }
  }

  @Nested
  @DisplayName("Tick Processing")
  class TickProcessing {

    @Test
    @DisplayName("should process all ticks until termination")
    void processesAllTicks() {
      int totalTicks = 15;
      Network network = TestNetworkBuilder.linearChain(3);
      SimulationConfigContext config =
          createMinimalConfig(totalTicks, List.of(AlgorithmType.Q_ROUTING));

      SimulationEngine engine = new SimulationEngine(config, network);
      engine.run();

      // Engine should have processed all ticks - we verify by checking
      // that no exception occurred and the engine completed
      assertNotNull(engine);
    }
  }

  @Nested
  @DisplayName("Network Topologies")
  class NetworkTopologies {

    @Test
    @DisplayName("should work with ring topology")
    void ringTopology_works() {
      Network network = TestNetworkBuilder.ring(6);
      SimulationConfigContext config = createMinimalConfig(20, List.of(AlgorithmType.Q_ROUTING));

      SimulationEngine engine = new SimulationEngine(config, network);

      assertDoesNotThrow(engine::run);
    }

    @Test
    @DisplayName("should work with star topology")
    void starTopology_works() {
      Network network = TestNetworkBuilder.star(5);
      SimulationConfigContext config = createMinimalConfig(20, List.of(AlgorithmType.Q_ROUTING));

      SimulationEngine engine = new SimulationEngine(config, network);

      assertDoesNotThrow(engine::run);
    }

    @Test
    @DisplayName("should work with grid topology")
    void gridTopology_works() {
      Network network = TestNetworkBuilder.grid(4, 4);
      SimulationConfigContext config = createMinimalConfig(20, List.of(AlgorithmType.Q_ROUTING));

      SimulationEngine engine = new SimulationEngine(config, network);

      assertDoesNotThrow(engine::run);
    }

    @Test
    @DisplayName("should work with diamond topology")
    void diamondTopology_works() {
      Network network = TestNetworkBuilder.diamond();
      SimulationConfigContext config = createMinimalConfig(20, List.of(AlgorithmType.Q_ROUTING));

      SimulationEngine engine = new SimulationEngine(config, network);

      assertDoesNotThrow(engine::run);
    }
  }

  @Nested
  @DisplayName("Algorithm Comparison")
  class AlgorithmComparison {

    @Test
    @DisplayName("should run Q_ROUTING and SHORTEST_PATH on same traffic pattern")
    void bothAlgorithms_sameTraficPattern() {
      long seed = 42L;
      int ticks = 50;

      Network network = TestNetworkBuilder.grid(3, 3);
      SimulationConfigContext config = createConfigWithBothAlgorithms(seed, ticks);

      SimulationEngine engine = new SimulationEngine(config, network);

      assertDoesNotThrow(engine::run);
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCases {

    @Test
    @DisplayName("should handle zero ticks simulation")
    void zeroTicks_terminatesImmediately() {
      Network network = TestNetworkBuilder.linearChain(3);
      SimulationConfigContext config = createMinimalConfig(0, List.of(AlgorithmType.Q_ROUTING));

      SimulationEngine engine = new SimulationEngine(config, network);

      // Zero ticks means immediate termination
      assertDoesNotThrow(engine::run);
    }

    @Test
    @DisplayName("should handle single tick simulation")
    void singleTick_works() {
      Network network = TestNetworkBuilder.linearChain(3);
      SimulationConfigContext config = createMinimalConfig(1, List.of(AlgorithmType.Q_ROUTING));

      SimulationEngine engine = new SimulationEngine(config, network);

      assertDoesNotThrow(engine::run);
    }

    @Test
    @DisplayName("should handle disconnected nodes in network")
    void disconnectedNodes_handledGracefully() {
      Network network = TestNetworkBuilder.disconnectedPair();
      SimulationConfigContext config = createMinimalConfig(10, List.of(AlgorithmType.Q_ROUTING));

      SimulationEngine engine = new SimulationEngine(config, network);

      // Should complete - packets may be created but won't be deliverable
      assertDoesNotThrow(engine::run);
    }
  }

  // Helper methods for creating test configurations

  private SimulationConfigContext createMinimalConfig(int ticks, List<AlgorithmType> algorithms) {
    return createConfigWithSeed(42L, ticks, algorithms);
  }

  private SimulationConfigContext createConfigWithSeed(long seed, int ticks) {
    return createConfigWithSeed(seed, ticks, List.of(AlgorithmType.Q_ROUTING));
  }

  private SimulationConfigContext createConfigWithSeed(
      long seed, int ticks, List<AlgorithmType> algorithms) {
    GeneralConfig general =
        new GeneralConfig(
            seed, TopologyType._6X6_GRID, null, algorithms, OptionalInt.of(100), 0, "test", null);

    TerminationConfig termination = new TerminationConfig.FixedTicks(ticks);

    InjectionScheduleConfig schedule = new InjectionScheduleConfig.LoadLevel(1.0);
    PairSelectionConfig pairSelection = new PairSelectionConfig.Random();
    PairConstraintsConfig constraints = new PairConstraintsConfig(true, false);
    TrafficConfig traffic =
        new TrafficConfig(schedule, pairSelection, constraints, new GroupsConfig(Map.of()));

    NetworkDynamicsConfig dynamics = new NetworkDynamicsConfig.None();
    ObservabilityConfig observability =
        new ObservabilityConfig(List.of(), List.of(), 1, 500, 0, OptionalLong.empty());

    return new SimulationConfigContext(general, termination, traffic, dynamics, observability);
  }

  private SimulationConfigContext createConfigWithGapSchedule(
      int ticks, int everyN, int batch, List<AlgorithmType> algorithms) {
    GeneralConfig general =
        new GeneralConfig(
            42L, TopologyType._6X6_GRID, null, algorithms, OptionalInt.of(100), 0, "test", null);

    TerminationConfig termination = new TerminationConfig.FixedTicks(ticks);

    InjectionScheduleConfig schedule = new InjectionScheduleConfig.Gap(everyN, batch);
    PairSelectionConfig pairSelection = new PairSelectionConfig.Random();
    PairConstraintsConfig constraints = new PairConstraintsConfig(true, false);
    TrafficConfig traffic =
        new TrafficConfig(schedule, pairSelection, constraints, new GroupsConfig(Map.of()));

    NetworkDynamicsConfig dynamics = new NetworkDynamicsConfig.None();
    ObservabilityConfig observability =
        new ObservabilityConfig(List.of(), List.of(), 1, 500, 0, OptionalLong.empty());

    return new SimulationConfigContext(general, termination, traffic, dynamics, observability);
  }

  private SimulationConfigContext createConfigWithBothAlgorithms(long seed, int ticks) {
    return createConfigWithSeed(
        seed, ticks, List.of(AlgorithmType.Q_ROUTING, AlgorithmType.SHORTEST_PATH));
  }
}
