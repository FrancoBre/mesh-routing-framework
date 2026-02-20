package org.ungs.core.traffic.schedule;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ungs.core.config.InjectionScheduleConfig;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Network;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.testutil.MockEventSink;
import org.ungs.testutil.TestConfigBuilder;
import org.ungs.testutil.TestNetworkBuilder;

@DisplayName("Injection Schedule Presets")
class SchedulePresetsTest {

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
  @DisplayName("LoadLevelSchedulePreset")
  class LoadLevelScheduleTests {

    @Test
    @DisplayName("should inject exactly L packets when L is integer")
    void integerLoad_injectsExactCount() {
      LoadLevelSchedulePreset preset = new LoadLevelSchedulePreset();
      InjectionScheduleConfig config = new InjectionScheduleConfig.LoadLevel(5.0);
      InjectionSchedule schedule = preset.create(config);

      SimulationRuntimeContext ctx = createContext(42L);

      int count = schedule.packetsToInject(ctx);
      assertEquals(5, count);
    }

    @Test
    @DisplayName("should inject floor(L) packets deterministically")
    void fractionalLoad_injectsAtLeastFloor() {
      LoadLevelSchedulePreset preset = new LoadLevelSchedulePreset();
      InjectionScheduleConfig config = new InjectionScheduleConfig.LoadLevel(2.7);
      InjectionSchedule schedule = preset.create(config);

      // Over many trials, should inject 2 or 3
      int min = Integer.MAX_VALUE;
      int max = Integer.MIN_VALUE;

      for (int i = 0; i < 100; i++) {
        SimulationRuntimeContext ctx = createContext(i);
        int count = schedule.packetsToInject(ctx);
        min = Math.min(min, count);
        max = Math.max(max, count);
      }

      assertEquals(2, min, "Minimum should be floor(2.7) = 2");
      assertEquals(3, max, "Maximum should be ceil(2.7) = 3");
    }

    @Test
    @DisplayName("should inject 0 packets when L is 0")
    void zeroLoad_injectsZero() {
      LoadLevelSchedulePreset preset = new LoadLevelSchedulePreset();
      InjectionScheduleConfig config = new InjectionScheduleConfig.LoadLevel(0.0);
      InjectionSchedule schedule = preset.create(config);

      SimulationRuntimeContext ctx = createContext(42L);

      assertEquals(0, schedule.packetsToInject(ctx));
    }

    @Test
    @DisplayName("should use fractional part as probability for extra packet")
    void fractionalProbability_worksCorrectly() {
      LoadLevelSchedulePreset preset = new LoadLevelSchedulePreset();
      InjectionScheduleConfig config = new InjectionScheduleConfig.LoadLevel(0.5);
      InjectionSchedule schedule = preset.create(config);

      // With L=0.5, should inject 0 or 1 with ~50% probability each
      int zeroCount = 0;
      int oneCount = 0;
      int trials = 1000;

      for (int i = 0; i < trials; i++) {
        SimulationRuntimeContext ctx = createContext(i);
        int count = schedule.packetsToInject(ctx);
        if (count == 0) zeroCount++;
        else if (count == 1) oneCount++;
      }

      // Both should be reasonably close to 500 (50%)
      assertTrue(
          zeroCount > 300 && zeroCount < 700, "Zero count should be around 50%: " + zeroCount);
      assertTrue(oneCount > 300 && oneCount < 700, "One count should be around 50%: " + oneCount);
    }

    @Test
    @DisplayName("should return correct type")
    void type_returnsLoadLevel() {
      LoadLevelSchedulePreset preset = new LoadLevelSchedulePreset();
      assertEquals(InjectionScheduleType.LOAD_LEVEL, preset.type());
    }
  }

  @Nested
  @DisplayName("ProbPerTickSchedulePreset")
  class ProbPerTickScheduleTests {

    @Test
    @DisplayName("should inject 1 packet when RNG < p")
    void probMet_injectsOne() {
      ProbPerTickSchedulePreset preset = new ProbPerTickSchedulePreset();
      InjectionScheduleConfig config = new InjectionScheduleConfig.ProbPerTick(1.0); // Always
      InjectionSchedule schedule = preset.create(config);

      SimulationRuntimeContext ctx = createContext(42L);

      assertEquals(1, schedule.packetsToInject(ctx));
    }

    @Test
    @DisplayName("should inject 0 packets when p is 0")
    void probZero_injectsZero() {
      ProbPerTickSchedulePreset preset = new ProbPerTickSchedulePreset();
      InjectionScheduleConfig config = new InjectionScheduleConfig.ProbPerTick(0.0);
      InjectionSchedule schedule = preset.create(config);

      SimulationRuntimeContext ctx = createContext(42L);

      assertEquals(0, schedule.packetsToInject(ctx));
    }

    @Test
    @DisplayName("should inject based on Bernoulli distribution")
    void bernoulliInjection_matchesProbability() {
      ProbPerTickSchedulePreset preset = new ProbPerTickSchedulePreset();
      InjectionScheduleConfig config = new InjectionScheduleConfig.ProbPerTick(0.3);
      InjectionSchedule schedule = preset.create(config);

      int injected = 0;
      int trials = 1000;

      for (int i = 0; i < trials; i++) {
        SimulationRuntimeContext ctx = createContext(i);
        injected += schedule.packetsToInject(ctx);
      }

      // Should be around 30% of trials
      double rate = (double) injected / trials;
      assertTrue(rate > 0.2 && rate < 0.4, "Injection rate should be around 0.3: " + rate);
    }

    @Test
    @DisplayName("should return correct type")
    void type_returnsProbPerTick() {
      ProbPerTickSchedulePreset preset = new ProbPerTickSchedulePreset();
      assertEquals(InjectionScheduleType.PROB_PER_TICK, preset.type());
    }
  }

  @Nested
  @DisplayName("GapSchedulePreset")
  class GapScheduleTests {

    @Test
    @DisplayName("should inject batch at tick 0")
    void tickZero_injectsBatch() {
      GapSchedulePreset preset = new GapSchedulePreset();
      InjectionScheduleConfig config = new InjectionScheduleConfig.Gap(5, 3);
      InjectionSchedule schedule = preset.create(config);

      SimulationRuntimeContext ctx = createContext(42L);
      // tick = 0

      assertEquals(3, schedule.packetsToInject(ctx));
    }

    @Test
    @DisplayName("should inject batch every N ticks")
    void everyNTicks_injectsBatch() {
      GapSchedulePreset preset = new GapSchedulePreset();
      InjectionScheduleConfig config = new InjectionScheduleConfig.Gap(5, 3);
      InjectionSchedule schedule = preset.create(config);

      SimulationRuntimeContext ctx = createContext(42L);

      // Tick 0: inject
      assertEquals(3, schedule.packetsToInject(ctx));

      // Tick 1-4: no inject
      for (int i = 1; i < 5; i++) {
        ctx.advanceOneTick();
        assertEquals(0, schedule.packetsToInject(ctx), "Tick " + i + " should not inject");
      }

      // Tick 5: inject again
      ctx.advanceOneTick();
      assertEquals(3, schedule.packetsToInject(ctx));
    }

    @Test
    @DisplayName("should respect batch size parameter")
    void batchSize_respected() {
      GapSchedulePreset preset = new GapSchedulePreset();
      InjectionScheduleConfig config = new InjectionScheduleConfig.Gap(1, 10);
      InjectionSchedule schedule = preset.create(config);

      SimulationRuntimeContext ctx = createContext(42L);

      assertEquals(10, schedule.packetsToInject(ctx));
    }

    @Test
    @DisplayName("should return 0 when batch size is 0")
    void zeroBatch_injectsZero() {
      GapSchedulePreset preset = new GapSchedulePreset();
      InjectionScheduleConfig config = new InjectionScheduleConfig.Gap(1, 0);
      InjectionSchedule schedule = preset.create(config);

      SimulationRuntimeContext ctx = createContext(42L);

      assertEquals(0, schedule.packetsToInject(ctx));
    }

    @Test
    @DisplayName("should return correct type")
    void type_returnsGap() {
      GapSchedulePreset preset = new GapSchedulePreset();
      assertEquals(InjectionScheduleType.GAP, preset.type());
    }
  }
}
