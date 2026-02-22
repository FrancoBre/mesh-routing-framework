package org.ungs.testutil;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;
import lombok.experimental.UtilityClass;
import org.ungs.core.config.*;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.core.topology.api.TopologyType;

@UtilityClass
public final class TestConfigBuilder {

  private static final long DEFAULT_SEED = 42L;
  private static final int DEFAULT_TICKS = 100;
  private static final int DEFAULT_MAX_ACTIVE_PACKETS = 1000;

  public static SimulationConfigContext minimal() {
    return withAlgorithms(AlgorithmType.Q_ROUTING);
  }

  public static SimulationConfigContext withAlgorithms(AlgorithmType... algorithms) {
    return withAlgorithmsAndTicks(DEFAULT_TICKS, algorithms);
  }

  public static SimulationConfigContext withAlgorithmsAndTicks(
      int ticks, AlgorithmType... algorithms) {
    GeneralConfig general =
        new GeneralConfig(
            DEFAULT_SEED,
            TopologyType._6X6_GRID,
            null,
            List.of(algorithms),
            OptionalInt.of(DEFAULT_MAX_ACTIVE_PACKETS),
            0,
            "test-experiment",
            null);

    TerminationConfig termination = new TerminationConfig.FixedTicks(ticks);

    TrafficConfig traffic = createDefaultTrafficConfig();

    NetworkDynamicsConfig dynamics = new NetworkDynamicsConfig.None();

    ObservabilityConfig observability =
        new ObservabilityConfig(List.of(), List.of(), 1, 500, 0, OptionalLong.empty());

    return new SimulationConfigContext(general, termination, traffic, dynamics, observability);
  }

  public static SimulationConfigContext withSeed(long seed) {
    GeneralConfig general =
        new GeneralConfig(
            seed,
            TopologyType._6X6_GRID,
            null,
            List.of(AlgorithmType.Q_ROUTING),
            OptionalInt.of(DEFAULT_MAX_ACTIVE_PACKETS),
            0,
            "test-experiment",
            null);

    TerminationConfig termination = new TerminationConfig.FixedTicks(DEFAULT_TICKS);

    TrafficConfig traffic = createDefaultTrafficConfig();

    NetworkDynamicsConfig dynamics = new NetworkDynamicsConfig.None();

    ObservabilityConfig observability =
        new ObservabilityConfig(List.of(), List.of(), 1, 500, 0, OptionalLong.empty());

    return new SimulationConfigContext(general, termination, traffic, dynamics, observability);
  }

  private static TrafficConfig createDefaultTrafficConfig() {
    InjectionScheduleConfig schedule = new InjectionScheduleConfig.LoadLevel(1.0);
    PairSelectionConfig pairSelection = new PairSelectionConfig.Random();
    PairConstraintsConfig constraints = new PairConstraintsConfig(true, false);
    return new TrafficConfig(schedule, pairSelection, constraints, new GroupsConfig(Map.of()));
  }
}
