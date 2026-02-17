package org.ungs.core.observability;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ungs.core.network.Node;
import org.ungs.core.network.Packet;
import org.ungs.core.observability.events.HopEvent;
import org.ungs.core.observability.events.PacketDeliveredEvent;
import org.ungs.core.observability.events.PacketDepartedEvent;
import org.ungs.core.observability.events.TickEvent;
import org.ungs.core.routing.api.AlgorithmType;

@DisplayName("Simulation Events")
class EventsTest {

  @Nested
  @DisplayName("HopEvent")
  class HopEventTests {

    @Test
    @DisplayName("should store all fields correctly")
    void storesFieldsCorrectly() {
      Packet.Id packetId = new Packet.Id(42);
      Node.Id from = new Node.Id(1);
      Node.Id to = new Node.Id(2);
      long sentTick = 100L;
      long expectedReceiveTick = 101L;
      AlgorithmType algorithm = AlgorithmType.Q_ROUTING;

      HopEvent event = new HopEvent(packetId, from, to, sentTick, expectedReceiveTick, algorithm);

      assertEquals(packetId, event.packetId());
      assertEquals(from, event.from());
      assertEquals(to, event.to());
      assertEquals(sentTick, event.sentTick());
      assertEquals(expectedReceiveTick, event.expectedReceiveTick());
      assertEquals(algorithm, event.algorithm());
    }

    @Test
    @DisplayName("should be immutable (record)")
    void isImmutable() {
      HopEvent event =
          new HopEvent(
              new Packet.Id(1), new Node.Id(1), new Node.Id(2), 0L, 1L, AlgorithmType.Q_ROUTING);

      // Records are immutable - verify this by testing equality
      HopEvent copy =
          new HopEvent(
              event.packetId(),
              event.from(),
              event.to(),
              event.sentTick(),
              event.expectedReceiveTick(),
              event.algorithm());

      assertEquals(event, copy);
    }

    @Test
    @DisplayName("should implement equals correctly")
    void equalsWorks() {
      HopEvent event1 =
          new HopEvent(
              new Packet.Id(1), new Node.Id(1), new Node.Id(2), 10L, 11L, AlgorithmType.Q_ROUTING);

      HopEvent event2 =
          new HopEvent(
              new Packet.Id(1), new Node.Id(1), new Node.Id(2), 10L, 11L, AlgorithmType.Q_ROUTING);

      HopEvent event3 =
          new HopEvent(
              new Packet.Id(2), // different packet ID
              new Node.Id(1),
              new Node.Id(2),
              10L,
              11L,
              AlgorithmType.Q_ROUTING);

      assertEquals(event1, event2);
      assertNotEquals(event1, event3);
    }
  }

  @Nested
  @DisplayName("PacketDeliveredEvent")
  class PacketDeliveredEventTests {

    @Test
    @DisplayName("should store all fields correctly")
    void storesFieldsCorrectly() {
      Packet packet = new Packet(new Packet.Id(42), new Node.Id(0), new Node.Id(5));
      int pathHopCount = 5;
      double receivedTime = 100.0;
      AlgorithmType algorithm = AlgorithmType.SHORTEST_PATH;

      PacketDeliveredEvent event =
          new PacketDeliveredEvent(packet, pathHopCount, receivedTime, algorithm);

      assertSame(packet, event.packet());
      assertEquals(pathHopCount, event.pathHopCount());
      assertEquals(receivedTime, event.receivedTime(), 0.001);
      assertEquals(algorithm, event.algorithm());
    }

    @Test
    @DisplayName("should be immutable (record)")
    void isImmutable() {
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(5));
      PacketDeliveredEvent event =
          new PacketDeliveredEvent(packet, 3, 50.0, AlgorithmType.Q_ROUTING);

      PacketDeliveredEvent copy =
          new PacketDeliveredEvent(
              event.packet(), event.pathHopCount(), event.receivedTime(), event.algorithm());

      assertEquals(event.packet(), copy.packet());
      assertEquals(event.pathHopCount(), copy.pathHopCount());
    }

    @Test
    @DisplayName("should handle zero hop count")
    void handlesZeroHopCount() {
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(0));
      PacketDeliveredEvent event =
          new PacketDeliveredEvent(packet, 0, 10.0, AlgorithmType.Q_ROUTING);

      assertEquals(0, event.pathHopCount());
    }
  }

  @Nested
  @DisplayName("PacketDepartedEvent")
  class PacketDepartedEventTests {

    @Test
    @DisplayName("should store all fields correctly")
    void storesFieldsCorrectly() {
      Packet.Id packetId = new Packet.Id(42);
      Node.Id from = new Node.Id(3);
      double departedTick = 25.0;
      AlgorithmType algorithm = AlgorithmType.Q_ROUTING;

      PacketDepartedEvent event = new PacketDepartedEvent(packetId, from, departedTick, algorithm);

      assertEquals(packetId, event.packetId());
      assertEquals(from, event.from());
      assertEquals(departedTick, event.departedTick(), 0.001);
      assertEquals(algorithm, event.algorithm());
    }

    @Test
    @DisplayName("should be immutable (record)")
    void isImmutable() {
      PacketDepartedEvent event =
          new PacketDepartedEvent(
              new Packet.Id(1), new Node.Id(0), 5.0, AlgorithmType.SHORTEST_PATH);

      PacketDepartedEvent copy =
          new PacketDepartedEvent(
              event.packetId(), event.from(), event.departedTick(), event.algorithm());

      assertEquals(event, copy);
    }

    @Test
    @DisplayName("should implement equals correctly")
    void equalsWorks() {
      PacketDepartedEvent event1 =
          new PacketDepartedEvent(new Packet.Id(1), new Node.Id(0), 5.0, AlgorithmType.Q_ROUTING);

      PacketDepartedEvent event2 =
          new PacketDepartedEvent(new Packet.Id(1), new Node.Id(0), 5.0, AlgorithmType.Q_ROUTING);

      assertEquals(event1, event2);
    }
  }

  @Nested
  @DisplayName("TickEvent")
  class TickEventTests {

    @Test
    @DisplayName("should store all fields correctly")
    void storesFieldsCorrectly() {
      double tick = 50.0;
      AlgorithmType algorithm = AlgorithmType.Q_ROUTING;
      int packetsInFlight = 25;
      long deliveredCount = 100L;
      long sentThisTick = 3L;

      TickEvent event =
          new TickEvent(tick, algorithm, packetsInFlight, deliveredCount, sentThisTick);

      assertEquals(tick, event.tick(), 0.001);
      assertEquals(algorithm, event.algorithm());
      assertEquals(packetsInFlight, event.packetsInFlight());
      assertEquals(deliveredCount, event.deliveredCount());
      assertEquals(sentThisTick, event.sentThisTick());
    }

    @Test
    @DisplayName("should be immutable (record)")
    void isImmutable() {
      TickEvent event = new TickEvent(10.0, AlgorithmType.SHORTEST_PATH, 5, 20L, 2L);

      TickEvent copy =
          new TickEvent(
              event.tick(),
              event.algorithm(),
              event.packetsInFlight(),
              event.deliveredCount(),
              event.sentThisTick());

      assertEquals(event, copy);
    }

    @Test
    @DisplayName("should handle zero counts")
    void handlesZeroCounts() {
      TickEvent event = new TickEvent(0.0, AlgorithmType.Q_ROUTING, 0, 0L, 0L);

      assertEquals(0.0, event.tick(), 0.001);
      assertEquals(0, event.packetsInFlight());
      assertEquals(0L, event.deliveredCount());
      assertEquals(0L, event.sentThisTick());
    }

    @Test
    @DisplayName("should implement equals correctly")
    void equalsWorks() {
      TickEvent event1 = new TickEvent(10.0, AlgorithmType.Q_ROUTING, 5, 100L, 3L);

      TickEvent event2 = new TickEvent(10.0, AlgorithmType.Q_ROUTING, 5, 100L, 3L);

      TickEvent event3 =
          new TickEvent(
              10.0,
              AlgorithmType.SHORTEST_PATH, // different algorithm
              5,
              100L,
              3L);

      assertEquals(event1, event2);
      assertNotEquals(event1, event3);
    }
  }
}
