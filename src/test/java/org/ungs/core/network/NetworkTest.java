package org.ungs.core.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.observability.events.HopEvent;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.core.topology.api.TopologyListener;
import org.ungs.testutil.MockEventSink;
import org.ungs.testutil.TestConfigBuilder;
import org.ungs.testutil.TestNetworkBuilder;

@DisplayName("Network")
class NetworkTest {

  private Network network;

  @BeforeEach
  void setUp() {
    network = new Network();
  }

  @Nested
  @DisplayName("Node Management")
  class NodeManagement {

    @Test
    @DisplayName("should add node and be retrievable by ID")
    void addNode_shouldBeRetrievableById() {
      Node node = new Node(new Node.Id(5), new ArrayList<>(), network);
      network.addNode(node);

      Node retrieved = network.getNode(new Node.Id(5));
      assertSame(node, retrieved);
    }

    @Test
    @DisplayName("should sort nodes by ID ascending when added out of order")
    void addNode_sortsNodesByIdAscending() {
      Node node3 = new Node(new Node.Id(3), new ArrayList<>(), network);
      Node node1 = new Node(new Node.Id(1), new ArrayList<>(), network);
      Node node5 = new Node(new Node.Id(5), new ArrayList<>(), network);
      Node node2 = new Node(new Node.Id(2), new ArrayList<>(), network);

      network.addNode(node3);
      network.addNode(node1);
      network.addNode(node5);
      network.addNode(node2);

      List<Node> nodes = network.getNodes();
      assertEquals(4, nodes.size());
      assertEquals(1, nodes.get(0).getId().value());
      assertEquals(2, nodes.get(1).getId().value());
      assertEquals(3, nodes.get(2).getId().value());
      assertEquals(5, nodes.get(3).getId().value());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for unknown node ID")
    void getNode_throwsForUnknownId() {
      Node node = new Node(new Node.Id(1), new ArrayList<>(), network);
      network.addNode(node);

      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> network.getNode(new Node.Id(999)));
      assertTrue(ex.getMessage().contains("Node not found"));
    }

    @Test
    @DisplayName("should return defensive copy of nodes list")
    void getNodes_returnsDefensiveCopy() {
      Node node = new Node(new Node.Id(1), new ArrayList<>(), network);
      network.addNode(node);

      List<Node> nodes = network.getNodes();
      assertThrows(UnsupportedOperationException.class, () -> nodes.add(node));
    }

    @Test
    @DisplayName("should return empty list when no nodes added")
    void getNodes_returnsEmptyListWhenNoNodes() {
      assertTrue(network.getNodes().isEmpty());
    }
  }

  @Nested
  @DisplayName("Topology Listener")
  class TopologyListenerTests {

    @Test
    @DisplayName("should notify listener when node is added")
    void addNode_notifiesListeners() {
      List<Node> addedNodes = new ArrayList<>();
      TopologyListener listener = addedNodes::add;

      network.addTopologyListener(listener);

      Node node = new Node(new Node.Id(1), new ArrayList<>(), network);
      network.addNode(node);

      assertEquals(1, addedNodes.size());
      assertSame(node, addedNodes.getFirst());
    }

    @Test
    @DisplayName("should notify multiple listeners when node is added")
    void addNode_notifiesMultipleListeners() {
      List<Node> listener1Nodes = new ArrayList<>();
      List<Node> listener2Nodes = new ArrayList<>();

      network.addTopologyListener(listener1Nodes::add);
      network.addTopologyListener(listener2Nodes::add);

      Node node = new Node(new Node.Id(1), new ArrayList<>(), network);
      network.addNode(node);

      assertEquals(1, listener1Nodes.size());
      assertEquals(1, listener2Nodes.size());
    }
  }

  @Nested
  @DisplayName("Packet Transmission")
  class PacketTransmission {

    private MockEventSink eventSink;

    @BeforeEach
    void setUp() {
      network = TestNetworkBuilder.linearChain(3); // 0 - 1 - 2
      eventSink = new MockEventSink();
      SimulationRuntimeContext ctx =
          new SimulationRuntimeContext(TestConfigBuilder.minimal(), network, eventSink);
      ctx.reset(AlgorithmType.Q_ROUTING);
      network.setRuntimeContext(ctx);
    }

    @Test
    @DisplayName("should send packet between neighbors and emit HopEvent")
    void sendPacket_betweenNeighbors_emitsHopEvent() {
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(2));

      network.sendPacket(new Node.Id(0), new Node.Id(1), packet);

      List<HopEvent> hops = eventSink.getHopEvents();
      assertEquals(1, hops.size());
      assertEquals(0, hops.getFirst().from().value());
      assertEquals(1, hops.getFirst().to().value());
    }

    @Test
    @DisplayName("should queue packet in receiver node after send")
    void sendPacket_betweenNeighbors_queuesPacketInReceiver() {
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(2));

      network.sendPacket(new Node.Id(0), new Node.Id(1), packet);

      Node receiverNode = network.getNode(new Node.Id(1));
      assertEquals(1, receiverNode.getQueue().size());
      assertSame(packet, receiverNode.getQueue().peek());
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when sending to non-neighbor")
    void sendPacket_toNonNeighbor_throws() {
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(2));

      // Node 0 and Node 2 are not direct neighbors in a chain
      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> network.sendPacket(new Node.Id(0), new Node.Id(2), packet));
      assertTrue(ex.getMessage().contains("not neighbors"));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when sender node not found")
    void sendPacket_senderNotFound_throws() {
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(2));

      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> network.sendPacket(new Node.Id(999), new Node.Id(1), packet));
      assertTrue(ex.getMessage().contains("Sender node not found"));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when receiver node not found")
    void sendPacket_receiverNotFound_throws() {
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(2));

      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> network.sendPacket(new Node.Id(0), new Node.Id(999), packet));
      assertTrue(ex.getMessage().contains("Receiver node not found"));
    }
  }

  @Nested
  @DisplayName("Distance Calculation")
  class DistanceCalculation {

    @Test
    @DisplayName("should return 0 for distance to same node")
    void getDistanceTo_sameNode_returnsZero() {
      network = TestNetworkBuilder.linearChain(3);
      assertEquals(0, network.getDistanceTo(new Node.Id(1), new Node.Id(1)));
    }

    @Test
    @DisplayName("should return 1 for distance to direct neighbor")
    void getDistanceTo_directNeighbor_returnsOne() {
      network = TestNetworkBuilder.linearChain(3); // 0 - 1 - 2
      assertEquals(1, network.getDistanceTo(new Node.Id(0), new Node.Id(1)));
    }

    @Test
    @DisplayName("should return correct distance in linear chain")
    void getDistanceTo_linearChain_returnsCorrectDistance() {
      network = TestNetworkBuilder.linearChain(5); // 0 - 1 - 2 - 3 - 4
      assertEquals(4, network.getDistanceTo(new Node.Id(0), new Node.Id(4)));
      assertEquals(2, network.getDistanceTo(new Node.Id(1), new Node.Id(3)));
    }

    @Test
    @DisplayName("should return correct distance in ring topology")
    void getDistanceTo_ring_returnsShortestPath() {
      network = TestNetworkBuilder.ring(6); // 0 - 1 - 2 - 3 - 4 - 5 - 0
      // From 0 to 3: either 0-1-2-3 (3 hops) or 0-5-4-3 (3 hops)
      assertEquals(3, network.getDistanceTo(new Node.Id(0), new Node.Id(3)));
      // From 0 to 2: 0-1-2 (2 hops)
      assertEquals(2, network.getDistanceTo(new Node.Id(0), new Node.Id(2)));
    }

    @Test
    @DisplayName("should return MAX_VALUE for unreachable node")
    void getDistanceTo_unreachableNode_returnsMaxValue() {
      network = TestNetworkBuilder.disconnectedPair();
      assertEquals(Integer.MAX_VALUE, network.getDistanceTo(new Node.Id(0), new Node.Id(1)));
    }

    @Test
    @DisplayName("should use BFS to find shortest path in diamond topology")
    void getDistanceTo_diamond_findsShortestPath() {
      network = TestNetworkBuilder.diamond(); // 0 connects to 1 and 2, both connect to 3
      assertEquals(2, network.getDistanceTo(new Node.Id(0), new Node.Id(3)));
    }
  }

  @Nested
  @DisplayName("Packets In Flight")
  class PacketsInFlight {

    @Test
    @DisplayName("should return 0 when no packets in any queue")
    void packetsInFlight_noPackets_returnsZero() {
      network = TestNetworkBuilder.linearChain(3);
      assertEquals(0, network.packetsInFlight());
    }

    @Test
    @DisplayName("should count packets across all node queues")
    void packetsInFlight_withPackets_returnsTotalCount() {
      network = TestNetworkBuilder.linearChain(3);
      Node node0 = network.getNode(new Node.Id(0));
      Node node1 = network.getNode(new Node.Id(1));

      node0.receivePacket(new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(2)));
      node0.receivePacket(new Packet(new Packet.Id(2), new Node.Id(0), new Node.Id(2)));
      node1.receivePacket(new Packet(new Packet.Id(3), new Node.Id(1), new Node.Id(2)));

      assertEquals(3, network.packetsInFlight());
    }
  }

  @Nested
  @DisplayName("Neighbor Check")
  class NeighborCheck {

    @BeforeEach
    void setUp() {
      network = TestNetworkBuilder.linearChain(3); // 0 - 1 - 2
    }

    @Test
    @DisplayName("should return true for direct neighbors")
    void isNeighbor_directNeighbors_returnsTrue() {
      assertTrue(network.isNeighbor(new Node.Id(0), new Node.Id(1)));
      assertTrue(network.isNeighbor(new Node.Id(1), new Node.Id(0)));
      assertTrue(network.isNeighbor(new Node.Id(1), new Node.Id(2)));
    }

    @Test
    @DisplayName("should return false for non-neighbors")
    void isNeighbor_nonNeighbors_returnsFalse() {
      assertFalse(network.isNeighbor(new Node.Id(0), new Node.Id(2)));
    }

    @Test
    @DisplayName("should throw for unknown node in neighbor check")
    void isNeighbor_unknownNode_throws() {
      assertThrows(
          IllegalArgumentException.class,
          () -> network.isNeighbor(new Node.Id(0), new Node.Id(999)));
    }
  }
}
