package org.ungs.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ungs.core.network.Network;
import org.ungs.core.network.Node;
import org.ungs.core.network.Packet;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.testutil.MockEventSink;
import org.ungs.testutil.TestConfigBuilder;
import org.ungs.testutil.TestNetworkBuilder;

@DisplayName("SimulationRuntimeContext")
class SimulationRuntimeContextTest {

  private SimulationRuntimeContext ctx;
  private Network network;
  private MockEventSink eventSink;

  @BeforeEach
  void setUp() {
    network = TestNetworkBuilder.linearChain(5);
    eventSink = new MockEventSink();
    ctx = new SimulationRuntimeContext(TestConfigBuilder.withSeed(12345L), network, eventSink);
  }

  @Nested
  @DisplayName("Initialization")
  class Initialization {

    @Test
    @DisplayName("should initialize with tick 0")
    void initialTick_isZero() {
      assertEquals(0.0, ctx.getTick(), 0.001);
    }

    @Test
    @DisplayName("should have reference to network")
    void hasNetworkReference() {
      assertSame(network, ctx.getNetwork());
    }

    @Test
    @DisplayName("should have reference to event sink")
    void hasEventSinkReference() {
      assertSame(eventSink, ctx.getEventSink());
    }

    @Test
    @DisplayName("should have empty delivered packets list")
    void deliveredPackets_initiallyEmpty() {
      assertTrue(ctx.getDeliveredPackets().isEmpty());
    }

    @Test
    @DisplayName("should have empty not-delivered packets list")
    void notDeliveredPackets_initiallyEmpty() {
      assertTrue(ctx.getNotDeliveredPackets().isEmpty());
    }

    @Test
    @DisplayName("should have empty pending sends list")
    void pendingSends_initiallyEmpty() {
      assertTrue(ctx.getPendingSends().isEmpty());
    }

    @Test
    @DisplayName("should have RNG initialized")
    void rng_isInitialized() {
      assertNotNull(ctx.getRng());
    }
  }

  @Nested
  @DisplayName("Reset")
  class Reset {

    @BeforeEach
    void makeStateChanges() {
      ctx.reset(AlgorithmType.Q_ROUTING);
      ctx.advanceOneTick();
      ctx.advanceOneTick();
      ctx.advanceOneTick();
      ctx.getDeliveredPackets().add(new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(4)));
      ctx.getNotDeliveredPackets()
          .add(new Packet(new Packet.Id(2), new Node.Id(0), new Node.Id(4)));
    }

    @Test
    @DisplayName("should reset tick to 0")
    void reset_resetsTickToZero() {
      assertEquals(3.0, ctx.getTick(), 0.001);

      ctx.reset(AlgorithmType.SHORTEST_PATH);

      assertEquals(0.0, ctx.getTick(), 0.001);
    }

    @Test
    @DisplayName("should update current algorithm")
    void reset_updatesAlgorithm() {
      assertEquals(AlgorithmType.Q_ROUTING, ctx.getCurrentAlgorithm());

      ctx.reset(AlgorithmType.SHORTEST_PATH);

      assertEquals(AlgorithmType.SHORTEST_PATH, ctx.getCurrentAlgorithm());
    }

    @Test
    @DisplayName("should clear delivered packets list")
    void reset_clearsDeliveredPackets() {
      assertFalse(ctx.getDeliveredPackets().isEmpty());

      ctx.reset(AlgorithmType.SHORTEST_PATH);

      assertTrue(ctx.getDeliveredPackets().isEmpty());
    }

    @Test
    @DisplayName("should clear not-delivered packets list")
    void reset_clearsNotDeliveredPackets() {
      assertFalse(ctx.getNotDeliveredPackets().isEmpty());

      ctx.reset(AlgorithmType.SHORTEST_PATH);

      assertTrue(ctx.getNotDeliveredPackets().isEmpty());
    }

    @Test
    @DisplayName("should reset RNG to produce same sequence as initial state")
    void reset_resetsRng() {
      // Get initial sequence from fresh context
      SimulationRuntimeContext fresh =
          new SimulationRuntimeContext(TestConfigBuilder.withSeed(12345L), network, eventSink);
      fresh.reset(AlgorithmType.Q_ROUTING);

      List<Long> freshSequence =
          List.of(fresh.getRng().nextLong(), fresh.getRng().nextLong(), fresh.getRng().nextLong());

      // Consume some RNG values
      ctx.getRng().nextLong();
      ctx.getRng().nextLong();

      // Reset and verify same sequence
      ctx.reset(AlgorithmType.SHORTEST_PATH);

      List<Long> resetSequence =
          List.of(ctx.getRng().nextLong(), ctx.getRng().nextLong(), ctx.getRng().nextLong());

      assertEquals(freshSequence, resetSequence);
    }

    @Test
    @DisplayName("should reset packet ID counter")
    void reset_resetsPacketIdCounter() {
      // Generate some packet IDs
      ctx.nextPacketId();
      ctx.nextPacketId();
      ctx.nextPacketId();

      ctx.reset(AlgorithmType.SHORTEST_PATH);

      // After reset, should start from 0 again
      assertEquals(0, ctx.nextPacketId().value());
    }
  }

  @Nested
  @DisplayName("Tick Advancement")
  class TickAdvancement {

    @Test
    @DisplayName("should advance tick by 1.0")
    void advanceOneTick_incrementsByOne() {
      assertEquals(0.0, ctx.getTick(), 0.001);

      ctx.advanceOneTick();

      assertEquals(1.0, ctx.getTick(), 0.001);
    }

    @Test
    @DisplayName("should accumulate tick advances")
    void advanceOneTick_accumulates() {
      ctx.advanceOneTick();
      ctx.advanceOneTick();
      ctx.advanceOneTick();
      ctx.advanceOneTick();
      ctx.advanceOneTick();

      assertEquals(5.0, ctx.getTick(), 0.001);
    }
  }

  @Nested
  @DisplayName("RNG Determinism")
  class RngDeterminism {

    @Test
    @DisplayName("should produce same sequence with same seed")
    void sameSeed_produceSameSequence() {
      SimulationRuntimeContext ctx1 =
          new SimulationRuntimeContext(TestConfigBuilder.withSeed(42L), network, eventSink);
      SimulationRuntimeContext ctx2 =
          new SimulationRuntimeContext(TestConfigBuilder.withSeed(42L), network, eventSink);
      ctx1.reset(AlgorithmType.Q_ROUTING);
      ctx2.reset(AlgorithmType.Q_ROUTING);

      for (int i = 0; i < 100; i++) {
        assertEquals(ctx1.getRng().nextLong(), ctx2.getRng().nextLong());
      }
    }

    @Test
    @DisplayName("should produce different sequence with different seed")
    void differentSeed_produceDifferentSequence() {
      SimulationRuntimeContext ctx1 =
          new SimulationRuntimeContext(TestConfigBuilder.withSeed(42L), network, eventSink);
      SimulationRuntimeContext ctx2 =
          new SimulationRuntimeContext(TestConfigBuilder.withSeed(43L), network, eventSink);
      ctx1.reset(AlgorithmType.Q_ROUTING);
      ctx2.reset(AlgorithmType.Q_ROUTING);

      // At least one value should differ in first 10
      boolean foundDifference = false;
      for (int i = 0; i < 10; i++) {
        if (ctx1.getRng().nextLong() != ctx2.getRng().nextLong()) {
          foundDifference = true;
          break;
        }
      }
      assertTrue(foundDifference);
    }
  }

  @Nested
  @DisplayName("Packet ID Generation")
  class PacketIdGeneration {

    @Test
    @DisplayName("should generate sequential packet IDs starting from 0")
    void nextPacketId_startsFromZero() {
      ctx.reset(AlgorithmType.Q_ROUTING);
      assertEquals(0, ctx.nextPacketId().value());
    }

    @Test
    @DisplayName("should increment packet ID with each call")
    void nextPacketId_increments() {
      ctx.reset(AlgorithmType.Q_ROUTING);

      assertEquals(0, ctx.nextPacketId().value());
      assertEquals(1, ctx.nextPacketId().value());
      assertEquals(2, ctx.nextPacketId().value());
      assertEquals(3, ctx.nextPacketId().value());
    }

    @Test
    @DisplayName("should generate unique IDs across many calls")
    void nextPacketId_generatesUniqueIds() {
      ctx.reset(AlgorithmType.Q_ROUTING);
      Set<Integer> ids = new HashSet<>();

      for (int i = 0; i < 1000; i++) {
        int id = ctx.nextPacketId().value();
        assertFalse(ids.contains(id), "Duplicate ID: " + id);
        ids.add(id);
      }
    }
  }

  @Nested
  @DisplayName("Pending Sends")
  class PendingSends {

    @Test
    @DisplayName("should schedule packet for sending")
    void schedule_addsToPendingSends() {
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(4));

      ctx.schedule(new Node.Id(0), new Node.Id(1), packet);

      assertEquals(1, ctx.getPendingSends().size());
    }

    @Test
    @DisplayName("should accumulate multiple scheduled sends")
    void schedule_accumulatesMultipleSends() {
      Packet p1 = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(4));
      Packet p2 = new Packet(new Packet.Id(2), new Node.Id(1), new Node.Id(4));
      Packet p3 = new Packet(new Packet.Id(3), new Node.Id(2), new Node.Id(4));

      ctx.schedule(new Node.Id(0), new Node.Id(1), p1);
      ctx.schedule(new Node.Id(1), new Node.Id(2), p2);
      ctx.schedule(new Node.Id(2), new Node.Id(3), p3);

      assertEquals(3, ctx.getPendingSends().size());
    }

    @Test
    @DisplayName("should return and clear pending sends on flush")
    void flushPendingSends_returnsAndClears() {
      Packet p1 = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(4));
      Packet p2 = new Packet(new Packet.Id(2), new Node.Id(1), new Node.Id(4));

      ctx.schedule(new Node.Id(0), new Node.Id(1), p1);
      ctx.schedule(new Node.Id(1), new Node.Id(2), p2);

      List<SimulationRuntimeContext.PendingSend> flushed = ctx.flushPendingSends();

      assertEquals(2, flushed.size());
      assertTrue(ctx.getPendingSends().isEmpty());
    }

    @Test
    @DisplayName("should return empty list on flush when no pending sends")
    void flushPendingSends_emptyWhenNoPending() {
      List<SimulationRuntimeContext.PendingSend> flushed = ctx.flushPendingSends();
      assertTrue(flushed.isEmpty());
    }

    @Test
    @DisplayName("should preserve send order on flush")
    void flushPendingSends_preservesOrder() {
      Packet p1 = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(4));
      Packet p2 = new Packet(new Packet.Id(2), new Node.Id(1), new Node.Id(4));
      Packet p3 = new Packet(new Packet.Id(3), new Node.Id(2), new Node.Id(4));

      ctx.schedule(new Node.Id(0), new Node.Id(1), p1);
      ctx.schedule(new Node.Id(1), new Node.Id(2), p2);
      ctx.schedule(new Node.Id(2), new Node.Id(3), p3);

      List<SimulationRuntimeContext.PendingSend> flushed = ctx.flushPendingSends();

      assertSame(p1, flushed.get(0).packet());
      assertSame(p2, flushed.get(1).packet());
      assertSame(p3, flushed.get(2).packet());
    }

    @Test
    @DisplayName("should allow scheduling more sends after flush")
    void flushPendingSends_allowsNewSchedulesAfterFlush() {
      Packet p1 = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(4));
      ctx.schedule(new Node.Id(0), new Node.Id(1), p1);
      ctx.flushPendingSends();

      Packet p2 = new Packet(new Packet.Id(2), new Node.Id(1), new Node.Id(4));
      ctx.schedule(new Node.Id(1), new Node.Id(2), p2);

      assertEquals(1, ctx.getPendingSends().size());
    }
  }

  @Nested
  @DisplayName("PendingSend Record")
  class PendingSendRecord {

    @Test
    @DisplayName("should store from, to, and packet correctly")
    void pendingSend_storesFields() {
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(4));
      SimulationRuntimeContext.PendingSend send =
          new SimulationRuntimeContext.PendingSend(new Node.Id(0), new Node.Id(1), packet);

      assertEquals(0, send.from().value());
      assertEquals(1, send.to().value());
      assertSame(packet, send.packet());
    }
  }

  @Nested
  @DisplayName("Config Access")
  class ConfigAccess {

    @Test
    @DisplayName("should return config reference")
    void getConfig_returnsConfig() {
      assertNotNull(ctx.getConfig());
    }
  }
}
