package org.ungs.core.network;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.observability.events.PacketDepartedEvent;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.testutil.MockEventSink;
import org.ungs.testutil.TestConfigBuilder;
import org.ungs.testutil.TestNetworkBuilder;

@DisplayName("Packet")
class PacketTest {

  private Packet packet;

  @BeforeEach
  void setUp() {
    packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(5));
  }

  @Nested
  @DisplayName("Construction")
  class Construction {

    @Test
    @DisplayName("should initialize with correct ID, origin, and destination")
    void constructor_setsBasicFields() {
      assertEquals(1, packet.getId().value());
      assertEquals(0, packet.getOrigin().value());
      assertEquals(5, packet.getDestination().value());
    }

    @Test
    @DisplayName("should initialize timeInQueue to 0")
    void constructor_initializesTimeInQueueToZero() {
      assertEquals(0.0, packet.getTimeInQueue(), 0.001);
    }

    @Test
    @DisplayName("should initialize departureTime to -1 (not departed)")
    void constructor_initializesDepartureTimeToNegativeOne() {
      assertEquals(-1.0, packet.getDepartureTime(), 0.001);
    }

    @Test
    @DisplayName("should initialize arrivalTime to 0")
    void constructor_initializesArrivalTimeToZero() {
      assertEquals(0.0, packet.getArrivalTime(), 0.001);
    }
  }

  @Nested
  @DisplayName("Time In Queue Tracking")
  class TimeInQueueTracking {

    @Test
    @DisplayName("should increment timeInQueue by 1")
    void incrementTimeInQueue_addsOne() {
      packet.incrementTimeInQueue();
      assertEquals(1.0, packet.getTimeInQueue(), 0.001);
    }

    @Test
    @DisplayName("should accumulate time across multiple increments")
    void incrementTimeInQueue_accumulates() {
      packet.incrementTimeInQueue();
      packet.incrementTimeInQueue();
      packet.incrementTimeInQueue();
      assertEquals(3.0, packet.getTimeInQueue(), 0.001);
    }

    @Test
    @DisplayName("should start from zero on new packet")
    void incrementTimeInQueue_startsFromZero() {
      assertEquals(0.0, packet.getTimeInQueue(), 0.001);
      packet.incrementTimeInQueue();
      assertEquals(1.0, packet.getTimeInQueue(), 0.001);
    }
  }

  @Nested
  @DisplayName("Departure Time Tracking")
  class DepartureTimeTracking {

    private SimulationRuntimeContext ctx;
    private MockEventSink eventSink;

    @BeforeEach
    void setUp() {
      Network network = TestNetworkBuilder.linearChain(3);
      eventSink = new MockEventSink();
      ctx = new SimulationRuntimeContext(TestConfigBuilder.minimal(), network, eventSink);
      ctx.reset(AlgorithmType.Q_ROUTING);
    }

    @Test
    @DisplayName("should set departure time on first markAsDeparted call")
    void markAsDeparted_setsDepartureTime() {
      ctx.advanceOneTick(); // tick = 1
      ctx.advanceOneTick(); // tick = 2
      ctx.advanceOneTick(); // tick = 3

      packet.markAsDeparted(ctx);

      assertEquals(3.0, packet.getDepartureTime(), 0.001);
    }

    @Test
    @DisplayName("should not update departure time on subsequent calls")
    void markAsDeparted_doesNotUpdateOnSubsequentCalls() {
      ctx.advanceOneTick(); // tick = 1
      packet.markAsDeparted(ctx);

      ctx.advanceOneTick(); // tick = 2
      ctx.advanceOneTick(); // tick = 3
      packet.markAsDeparted(ctx);

      assertEquals(1.0, packet.getDepartureTime(), 0.001);
    }

    @Test
    @DisplayName("should emit PacketDepartedEvent on departure")
    void markAsDeparted_emitsEvent() {
      ctx.advanceOneTick();
      ctx.advanceOneTick();

      packet.markAsDeparted(ctx);

      assertEquals(1, eventSink.getDepartedEvents().size());
      PacketDepartedEvent event = eventSink.getDepartedEvents().getFirst();
      assertEquals(packet.getId(), event.packetId());
      assertEquals(packet.getOrigin(), event.from());
      assertEquals(2.0, event.departedTick(), 0.001);
    }

    @Test
    @DisplayName("should emit event with correct algorithm type")
    void markAsDeparted_emitsEventWithCorrectAlgorithm() {
      ctx.reset(AlgorithmType.SHORTEST_PATH);

      packet.markAsDeparted(ctx);

      PacketDepartedEvent event = eventSink.getDepartedEvents().getFirst();
      assertEquals(AlgorithmType.SHORTEST_PATH, event.algorithm());
    }
  }

  @Nested
  @DisplayName("Arrival Time Tracking")
  class ArrivalTimeTracking {

    private SimulationRuntimeContext ctx;

    @BeforeEach
    void setUp() {
      Network network = TestNetworkBuilder.linearChain(3);
      MockEventSink eventSink = new MockEventSink();
      ctx = new SimulationRuntimeContext(TestConfigBuilder.minimal(), network, eventSink);
      ctx.reset(AlgorithmType.Q_ROUTING);
    }

    @Test
    @DisplayName("should set arrival time")
    void markAsArrived_setsArrivalTime() {
      ctx.advanceOneTick(); // tick = 1
      ctx.advanceOneTick(); // tick = 2
      ctx.advanceOneTick(); // tick = 3
      ctx.advanceOneTick(); // tick = 4
      ctx.advanceOneTick(); // tick = 5

      packet.markAsArrived(ctx);

      assertEquals(5.0, packet.getArrivalTime(), 0.001);
    }

    @Test
    @DisplayName("should update arrival time on subsequent calls")
    void markAsArrived_updatesOnSubsequentCalls() {
      ctx.advanceOneTick(); // tick = 1
      packet.markAsArrived(ctx);
      assertEquals(1.0, packet.getArrivalTime(), 0.001);

      ctx.advanceOneTick(); // tick = 2
      ctx.advanceOneTick(); // tick = 3
      packet.markAsArrived(ctx);

      assertEquals(3.0, packet.getArrivalTime(), 0.001);
    }
  }

  @Nested
  @DisplayName("Packet.Id Record")
  class PacketIdRecord {

    @Test
    @DisplayName("should be equal for same value")
    void id_equality_sameValue() {
      Packet.Id id1 = new Packet.Id(10);
      Packet.Id id2 = new Packet.Id(10);

      assertEquals(id1, id2);
      assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    @DisplayName("should not be equal for different values")
    void id_inequality_differentValue() {
      Packet.Id id1 = new Packet.Id(10);
      Packet.Id id2 = new Packet.Id(11);

      assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("should return correct value")
    void id_value_returnsCorrectValue() {
      Packet.Id id = new Packet.Id(42);
      assertEquals(42, id.value());
    }
  }

  @Nested
  @DisplayName("ToString")
  class ToStringTests {

    @Test
    @DisplayName("should contain packet ID in toString")
    void toString_containsId() {
      String str = packet.toString();
      assertTrue(str.contains("Packet"));
    }
  }

  @Nested
  @DisplayName("State Transitions")
  class StateTransitions {

    private SimulationRuntimeContext ctx;

    @BeforeEach
    void setUp() {
      Network network = TestNetworkBuilder.linearChain(3);
      MockEventSink eventSink = new MockEventSink();
      ctx = new SimulationRuntimeContext(TestConfigBuilder.minimal(), network, eventSink);
      ctx.reset(AlgorithmType.Q_ROUTING);
    }

    @Test
    @DisplayName("should track full packet lifecycle")
    void fullLifecycle_tracksCorrectTimes() {
      // Depart at tick 2
      ctx.advanceOneTick();
      ctx.advanceOneTick();
      packet.markAsDeparted(ctx);
      assertEquals(2.0, packet.getDepartureTime(), 0.001);

      // Spend time in queue
      packet.incrementTimeInQueue();
      packet.incrementTimeInQueue();
      packet.incrementTimeInQueue();
      assertEquals(3.0, packet.getTimeInQueue(), 0.001);

      // Arrive at tick 8
      ctx.advanceOneTick(); // 3
      ctx.advanceOneTick(); // 4
      ctx.advanceOneTick(); // 5
      ctx.advanceOneTick(); // 6
      ctx.advanceOneTick(); // 7
      ctx.advanceOneTick(); // 8
      packet.markAsArrived(ctx);
      assertEquals(8.0, packet.getArrivalTime(), 0.001);
    }

    @Test
    @DisplayName("should maintain independence between time counters")
    void counters_areIndependent() {
      ctx.advanceOneTick();
      packet.markAsDeparted(ctx);

      packet.incrementTimeInQueue();
      packet.incrementTimeInQueue();

      ctx.advanceOneTick();
      ctx.advanceOneTick();
      packet.markAsArrived(ctx);

      // Departure time is fixed at first call
      assertEquals(1.0, packet.getDepartureTime(), 0.001);
      // Queue time accumulates independently
      assertEquals(2.0, packet.getTimeInQueue(), 0.001);
      // Arrival time updates to current tick
      assertEquals(3.0, packet.getArrivalTime(), 0.001);
    }
  }
}
