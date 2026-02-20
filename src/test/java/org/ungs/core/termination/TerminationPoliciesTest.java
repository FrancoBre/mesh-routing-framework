package org.ungs.core.termination;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ungs.core.config.TerminationConfig;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Network;
import org.ungs.core.network.Node;
import org.ungs.core.network.Packet;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.core.termination.api.CompositeMode;
import org.ungs.core.termination.api.TerminationPolicy;
import org.ungs.core.termination.api.TerminationPolicyType;
import org.ungs.core.termination.presets.CompositeTerminationPreset;
import org.ungs.core.termination.presets.FixedTicksTerminationPreset;
import org.ungs.core.termination.presets.TotalPacketsDeliveredTerminationPreset;
import org.ungs.testutil.MockEventSink;
import org.ungs.testutil.TestConfigBuilder;
import org.ungs.testutil.TestNetworkBuilder;

@DisplayName("Termination Policies")
class TerminationPoliciesTest {

  private SimulationRuntimeContext ctx;

  @BeforeEach
  void setUp() {
    Network network = TestNetworkBuilder.linearChain(5);
    MockEventSink eventSink = new MockEventSink();
    ctx = new SimulationRuntimeContext(TestConfigBuilder.withSeed(42L), network, eventSink);
    ctx.reset(AlgorithmType.Q_ROUTING);
  }

  @Nested
  @DisplayName("FixedTicksTerminationPreset")
  class FixedTicksTests {

    @Test
    @DisplayName("should not stop before N ticks")
    void beforeNTicks_shouldNotStop() {
      FixedTicksTerminationPreset preset = new FixedTicksTerminationPreset();
      TerminationConfig config = new TerminationConfig.FixedTicks(10);
      TerminationPolicy policy = preset.create(config);

      // At tick 0
      assertFalse(policy.shouldStop(ctx));

      // At tick 9 (N-1)
      for (int i = 0; i < 9; i++) {
        ctx.advanceOneTick();
      }
      assertFalse(policy.shouldStop(ctx));
    }

    @Test
    @DisplayName("should stop at exactly N ticks")
    void atNTicks_shouldStop() {
      FixedTicksTerminationPreset preset = new FixedTicksTerminationPreset();
      TerminationConfig config = new TerminationConfig.FixedTicks(10);
      TerminationPolicy policy = preset.create(config);

      // Advance to tick 10
      for (int i = 0; i < 10; i++) {
        ctx.advanceOneTick();
      }

      assertTrue(policy.shouldStop(ctx));
    }

    @Test
    @DisplayName("should continue stopping after N ticks")
    void afterNTicks_shouldStop() {
      FixedTicksTerminationPreset preset = new FixedTicksTerminationPreset();
      TerminationConfig config = new TerminationConfig.FixedTicks(10);
      TerminationPolicy policy = preset.create(config);

      // Advance to tick 15 (N+5)
      for (int i = 0; i < 15; i++) {
        ctx.advanceOneTick();
      }

      assertTrue(policy.shouldStop(ctx));
    }

    @Test
    @DisplayName("should stop immediately when N is 0")
    void zeroTicks_stopsImmediately() {
      FixedTicksTerminationPreset preset = new FixedTicksTerminationPreset();
      TerminationConfig config = new TerminationConfig.FixedTicks(0);
      TerminationPolicy policy = preset.create(config);

      assertTrue(policy.shouldStop(ctx));
    }

    @Test
    @DisplayName("should return correct type")
    void type_returnsFixedTicks() {
      FixedTicksTerminationPreset preset = new FixedTicksTerminationPreset();
      assertEquals(TerminationPolicyType.FIXED_TICKS, preset.type());
    }

    @Test
    @DisplayName("boundary test: stop at tick N-1")
    void boundary_NMinusOne_doesNotStop() {
      FixedTicksTerminationPreset preset = new FixedTicksTerminationPreset();
      TerminationConfig config = new TerminationConfig.FixedTicks(5);
      TerminationPolicy policy = preset.create(config);

      for (int i = 0; i < 4; i++) {
        ctx.advanceOneTick();
      }
      // At tick 4 (5-1)
      assertFalse(policy.shouldStop(ctx));
    }

    @Test
    @DisplayName("boundary test: stop at tick N")
    void boundary_N_stops() {
      FixedTicksTerminationPreset preset = new FixedTicksTerminationPreset();
      TerminationConfig config = new TerminationConfig.FixedTicks(5);
      TerminationPolicy policy = preset.create(config);

      for (int i = 0; i < 5; i++) {
        ctx.advanceOneTick();
      }
      // At tick 5
      assertTrue(policy.shouldStop(ctx));
    }

    @Test
    @DisplayName("boundary test: stop at tick N+1")
    void boundary_NPlusOne_stops() {
      FixedTicksTerminationPreset preset = new FixedTicksTerminationPreset();
      TerminationConfig config = new TerminationConfig.FixedTicks(5);
      TerminationPolicy policy = preset.create(config);

      for (int i = 0; i < 6; i++) {
        ctx.advanceOneTick();
      }
      // At tick 6
      assertTrue(policy.shouldStop(ctx));
    }
  }

  @Nested
  @DisplayName("TotalPacketsDeliveredTerminationPreset")
  class TotalPacketsDeliveredTests {

    @Test
    @DisplayName("should not stop before N deliveries")
    void beforeNDeliveries_shouldNotStop() {
      TotalPacketsDeliveredTerminationPreset preset = new TotalPacketsDeliveredTerminationPreset();
      TerminationConfig config = new TerminationConfig.TotalPacketsDelivered(5);
      TerminationPolicy policy = preset.create(config);

      // Deliver 4 packets
      for (int i = 0; i < 4; i++) {
        ctx.getDeliveredPackets().add(createPacket(i));
      }

      assertFalse(policy.shouldStop(ctx));
    }

    @Test
    @DisplayName("should stop at exactly N deliveries")
    void atNDeliveries_shouldStop() {
      TotalPacketsDeliveredTerminationPreset preset = new TotalPacketsDeliveredTerminationPreset();
      TerminationConfig config = new TerminationConfig.TotalPacketsDelivered(5);
      TerminationPolicy policy = preset.create(config);

      // Deliver 5 packets
      for (int i = 0; i < 5; i++) {
        ctx.getDeliveredPackets().add(createPacket(i));
      }

      assertTrue(policy.shouldStop(ctx));
    }

    @Test
    @DisplayName("should continue stopping after N deliveries")
    void afterNDeliveries_shouldStop() {
      TotalPacketsDeliveredTerminationPreset preset = new TotalPacketsDeliveredTerminationPreset();
      TerminationConfig config = new TerminationConfig.TotalPacketsDelivered(5);
      TerminationPolicy policy = preset.create(config);

      // Deliver 10 packets
      for (int i = 0; i < 10; i++) {
        ctx.getDeliveredPackets().add(createPacket(i));
      }

      assertTrue(policy.shouldStop(ctx));
    }

    @Test
    @DisplayName("should not stop when no packets delivered")
    void zeroDeliveries_doesNotStop() {
      TotalPacketsDeliveredTerminationPreset preset = new TotalPacketsDeliveredTerminationPreset();
      TerminationConfig config = new TerminationConfig.TotalPacketsDelivered(5);
      TerminationPolicy policy = preset.create(config);

      assertFalse(policy.shouldStop(ctx));
    }

    @Test
    @DisplayName("should return correct type")
    void type_returnsTotalPacketsDelivered() {
      TotalPacketsDeliveredTerminationPreset preset = new TotalPacketsDeliveredTerminationPreset();
      assertEquals(TerminationPolicyType.TOTAL_PACKETS_DELIVERED, preset.type());
    }
  }

  @Nested
  @DisplayName("CompositeTerminationPreset - OR Mode")
  class CompositeOrModeTests {

    @Test
    @DisplayName("should stop when first policy says stop")
    void orMode_stopsOnFirstPolicy() {
      CompositeTerminationPreset preset = new CompositeTerminationPreset();
      TerminationConfig config =
          new TerminationConfig.Composite(
              CompositeMode.OR,
              List.of(
                  new TerminationConfig.FixedTicks(5),
                  new TerminationConfig.TotalPacketsDelivered(100)));
      TerminationPolicy policy = preset.create(config);

      // Advance to tick 5 (first policy triggers)
      for (int i = 0; i < 5; i++) {
        ctx.advanceOneTick();
      }

      assertTrue(policy.shouldStop(ctx));
    }

    @Test
    @DisplayName("should stop when second policy says stop")
    void orMode_stopsOnSecondPolicy() {
      CompositeTerminationPreset preset = new CompositeTerminationPreset();
      TerminationConfig config =
          new TerminationConfig.Composite(
              CompositeMode.OR,
              List.of(
                  new TerminationConfig.FixedTicks(100),
                  new TerminationConfig.TotalPacketsDelivered(5)));
      TerminationPolicy policy = preset.create(config);

      // Deliver 5 packets (second policy triggers)
      for (int i = 0; i < 5; i++) {
        ctx.getDeliveredPackets().add(createPacket(i));
      }

      assertTrue(policy.shouldStop(ctx));
    }

    @Test
    @DisplayName("should not stop when neither policy says stop")
    void orMode_doesNotStopWhenNeither() {
      CompositeTerminationPreset preset = new CompositeTerminationPreset();
      TerminationConfig config =
          new TerminationConfig.Composite(
              CompositeMode.OR,
              List.of(
                  new TerminationConfig.FixedTicks(100),
                  new TerminationConfig.TotalPacketsDelivered(100)));
      TerminationPolicy policy = preset.create(config);

      // Neither condition met
      ctx.advanceOneTick();
      ctx.getDeliveredPackets().add(createPacket(0));

      assertFalse(policy.shouldStop(ctx));
    }
  }

  @Nested
  @DisplayName("CompositeTerminationPreset - AND Mode")
  class CompositeAndModeTests {

    @Test
    @DisplayName("should not stop when only first policy says stop")
    void andMode_doesNotStopOnFirstOnly() {
      CompositeTerminationPreset preset = new CompositeTerminationPreset();
      TerminationConfig config =
          new TerminationConfig.Composite(
              CompositeMode.AND,
              List.of(
                  new TerminationConfig.FixedTicks(5),
                  new TerminationConfig.TotalPacketsDelivered(100)));
      TerminationPolicy policy = preset.create(config);

      // First policy triggers but not second
      for (int i = 0; i < 5; i++) {
        ctx.advanceOneTick();
      }

      assertFalse(policy.shouldStop(ctx));
    }

    @Test
    @DisplayName("should not stop when only second policy says stop")
    void andMode_doesNotStopOnSecondOnly() {
      CompositeTerminationPreset preset = new CompositeTerminationPreset();
      TerminationConfig config =
          new TerminationConfig.Composite(
              CompositeMode.AND,
              List.of(
                  new TerminationConfig.FixedTicks(100),
                  new TerminationConfig.TotalPacketsDelivered(5)));
      TerminationPolicy policy = preset.create(config);

      // Second policy triggers but not first
      for (int i = 0; i < 5; i++) {
        ctx.getDeliveredPackets().add(createPacket(i));
      }

      assertFalse(policy.shouldStop(ctx));
    }

    @Test
    @DisplayName("should stop when both policies say stop")
    void andMode_stopsWhenBoth() {
      CompositeTerminationPreset preset = new CompositeTerminationPreset();
      TerminationConfig config =
          new TerminationConfig.Composite(
              CompositeMode.AND,
              List.of(
                  new TerminationConfig.FixedTicks(5),
                  new TerminationConfig.TotalPacketsDelivered(3)));
      TerminationPolicy policy = preset.create(config);

      // Both conditions met
      for (int i = 0; i < 5; i++) {
        ctx.advanceOneTick();
      }
      for (int i = 0; i < 3; i++) {
        ctx.getDeliveredPackets().add(createPacket(i));
      }

      assertTrue(policy.shouldStop(ctx));
    }

    @Test
    @DisplayName("should not stop when neither policy says stop")
    void andMode_doesNotStopWhenNeither() {
      CompositeTerminationPreset preset = new CompositeTerminationPreset();
      TerminationConfig config =
          new TerminationConfig.Composite(
              CompositeMode.AND,
              List.of(
                  new TerminationConfig.FixedTicks(100),
                  new TerminationConfig.TotalPacketsDelivered(100)));
      TerminationPolicy policy = preset.create(config);

      assertFalse(policy.shouldStop(ctx));
    }
  }

  @Nested
  @DisplayName("CompositeTerminationPreset - Type")
  class CompositeTypeTests {

    @Test
    @DisplayName("should return correct type")
    void type_returnsComposite() {
      CompositeTerminationPreset preset = new CompositeTerminationPreset();
      assertEquals(TerminationPolicyType.COMPOSITE, preset.type());
    }
  }

  private Packet createPacket(int id) {
    return new Packet(new Packet.Id(id), new Node.Id(0), new Node.Id(4));
  }
}
