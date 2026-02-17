package org.ungs.core.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ungs.core.routing.api.RoutingApplication;
import org.ungs.core.routing.impl.qrouting.QRoutingApplication;
import org.ungs.testutil.TestNetworkBuilder;

@DisplayName("Node")
class NodeTest {

  private Network network;
  private Node node;

  @BeforeEach
  void setUp() {
    network = new Network();
    node = new Node(new Node.Id(1), new ArrayList<>(), network);
    network.addNode(node);
  }

  @Nested
  @DisplayName("Queue Operations")
  class QueueOperations {

    @Test
    @DisplayName("should receive packet and add to queue (FIFO)")
    void receivePacket_addsToQueueEnd() {
      Packet packet1 = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(2));
      Packet packet2 = new Packet(new Packet.Id(2), new Node.Id(0), new Node.Id(2));

      node.receivePacket(packet1);
      node.receivePacket(packet2);

      assertEquals(2, node.getQueue().size());
      assertSame(packet1, node.getQueue().peekFirst());
      assertSame(packet2, node.getQueue().peekLast());
    }

    @Test
    @DisplayName("should return packets in FIFO order with getNextPacket")
    void getNextPacket_returnsFIFOOrder() {
      Packet packet1 = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(2));
      Packet packet2 = new Packet(new Packet.Id(2), new Node.Id(0), new Node.Id(2));
      Packet packet3 = new Packet(new Packet.Id(3), new Node.Id(0), new Node.Id(2));

      node.receivePacket(packet1);
      node.receivePacket(packet2);
      node.receivePacket(packet3);

      assertSame(packet1, node.getNextPacket());
      assertSame(packet2, node.getNextPacket());
      assertSame(packet3, node.getNextPacket());
    }

    @Test
    @DisplayName("should return null when queue is empty")
    void getNextPacket_emptyQueue_returnsNull() {
      assertNull(node.getNextPacket());
    }

    @Test
    @DisplayName("should remove packet from queue on getNextPacket")
    void getNextPacket_removesFromQueue() {
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(2));
      node.receivePacket(packet);

      assertEquals(1, node.getQueue().size());
      node.getNextPacket();
      assertEquals(0, node.getQueue().size());
    }

    @Test
    @DisplayName("should clear queue with emptyQueue")
    void emptyQueue_clearsAllPackets() {
      node.receivePacket(new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(2)));
      node.receivePacket(new Packet(new Packet.Id(2), new Node.Id(0), new Node.Id(2)));
      node.receivePacket(new Packet(new Packet.Id(3), new Node.Id(0), new Node.Id(2)));

      assertEquals(3, node.getQueue().size());
      node.emptyQueue();
      assertEquals(0, node.getQueue().size());
    }

    @Test
    @DisplayName("should handle emptyQueue on already empty queue")
    void emptyQueue_alreadyEmpty_doesNotThrow() {
      assertDoesNotThrow(() -> node.emptyQueue());
      assertEquals(0, node.getQueue().size());
    }
  }

  @Nested
  @DisplayName("Application Installation")
  class ApplicationInstallation {

    @Test
    @DisplayName("should install routing application")
    void installApplication_setsApplication() {
      RoutingApplication app = new QRoutingApplication(node);
      node.installApplication(app);

      assertSame(app, node.getApplication());
    }

    @Test
    @DisplayName("should replace existing application")
    void installApplication_replacesExisting() {
      RoutingApplication app1 = new QRoutingApplication(node);
      RoutingApplication app2 = new QRoutingApplication(node);

      node.installApplication(app1);
      node.installApplication(app2);

      assertSame(app2, node.getApplication());
      assertNotSame(app1, node.getApplication());
    }

    @Test
    @DisplayName("should return null when no application installed")
    void getApplication_noInstallation_returnsNull() {
      assertNull(node.getApplication());
    }
  }

  @Nested
  @DisplayName("Neighbor Management")
  class NeighborManagement {

    @Test
    @DisplayName("should start with empty neighbors list when created with empty list")
    void constructor_emptyNeighbors() {
      assertTrue(node.getNeighbors().isEmpty());
    }

    @Test
    @DisplayName("should allow adding neighbors after construction")
    void neighbors_canBeAddedAfterConstruction() {
      Node neighbor = new Node(new Node.Id(2), new ArrayList<>(), network);
      network.addNode(neighbor);

      node.getNeighbors().add(neighbor);

      assertEquals(1, node.getNeighbors().size());
      assertSame(neighbor, node.getNeighbors().getFirst());
    }

    @Test
    @DisplayName("should return correct neighbors from predefined network")
    void getNeighbors_linearChain_returnsCorrectNeighbors() {
      Network chain = TestNetworkBuilder.linearChain(3); // 0 - 1 - 2
      Node middle = chain.getNode(new Node.Id(1));

      List<Node> neighbors = middle.getNeighbors();
      assertEquals(2, neighbors.size());

      List<Integer> neighborIds = neighbors.stream().map(n -> n.getId().value()).sorted().toList();
      assertEquals(List.of(0, 2), neighborIds);
    }

    @Test
    @DisplayName("should have single neighbor for end node in chain")
    void getNeighbors_endOfChain_hasSingleNeighbor() {
      Network chain = TestNetworkBuilder.linearChain(3); // 0 - 1 - 2
      Node endNode = chain.getNode(new Node.Id(0));

      assertEquals(1, endNode.getNeighbors().size());
      assertEquals(1, endNode.getNeighbors().getFirst().getId().value());
    }
  }

  @Nested
  @DisplayName("Node.Id Record")
  class NodeIdRecord {

    @Test
    @DisplayName("should be equal for same value")
    void id_equality_sameValue() {
      Node.Id id1 = new Node.Id(5);
      Node.Id id2 = new Node.Id(5);

      assertEquals(id1, id2);
      assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    @DisplayName("should not be equal for different values")
    void id_inequality_differentValue() {
      Node.Id id1 = new Node.Id(5);
      Node.Id id2 = new Node.Id(6);

      assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("should return correct value")
    void id_value_returnsCorrectValue() {
      Node.Id id = new Node.Id(42);
      assertEquals(42, id.value());
    }
  }

  @Nested
  @DisplayName("ToString")
  class ToStringTests {

    @Test
    @DisplayName("should contain node ID in toString")
    void toString_containsId() {
      Node testNode = new Node(new Node.Id(7), new ArrayList<>(), network);
      String str = testNode.toString();

      assertTrue(str.contains("7"));
      assertTrue(str.contains("Node"));
    }
  }

  @Nested
  @DisplayName("Network Reference")
  class NetworkReference {

    @Test
    @DisplayName("should return network reference")
    void getNetwork_returnsNetwork() {
      assertSame(network, node.getNetwork());
    }
  }
}
