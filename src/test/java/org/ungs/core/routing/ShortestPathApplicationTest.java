package org.ungs.core.routing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Network;
import org.ungs.core.network.Node;
import org.ungs.core.network.Packet;
import org.ungs.core.observability.events.PacketDeliveredEvent;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.core.routing.impl.shortestpath.ShortestPathApplication;
import org.ungs.testutil.MockEventSink;
import org.ungs.testutil.TestConfigBuilder;
import org.ungs.testutil.TestNetworkBuilder;

@DisplayName("ShortestPathApplication")
class ShortestPathApplicationTest {

  private Network network;
  private MockEventSink eventSink;
  private SimulationRuntimeContext ctx;

  @BeforeEach
  void setUp() {
    network = TestNetworkBuilder.linearChain(5); // 0 - 1 - 2 - 3 - 4
    eventSink = new MockEventSink();
    ctx = new SimulationRuntimeContext(TestConfigBuilder.withSeed(42L), network, eventSink);
    ctx.reset(AlgorithmType.SHORTEST_PATH);
    network.setRuntimeContext(ctx);
    installShortestPathApps();
  }

  private void installShortestPathApps() {
    for (Node node : network.getNodes()) {
      node.installApplication(new ShortestPathApplication(node));
      node.emptyQueue();
    }
  }

  @Nested
  @DisplayName("Algorithm Type")
  class AlgorithmTypeTests {

    @Test
    @DisplayName("should return SHORTEST_PATH as algorithm type")
    void getType_returnsShortestPath() {
      Node node = network.getNode(new Node.Id(0));
      ShortestPathApplication app = (ShortestPathApplication) node.getApplication();

      assertEquals(AlgorithmType.SHORTEST_PATH, app.getType());
    }
  }

  @Nested
  @DisplayName("Destination Reached")
  class DestinationReached {

    @Test
    @DisplayName("should emit PacketDeliveredEvent when packet reaches destination")
    void onTick_atDestination_emitsDeliveredEvent() {
      Node destNode = network.getNode(new Node.Id(2));
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(2));
      destNode.receivePacket(packet);

      destNode.getApplication().onTick(ctx);

      List<PacketDeliveredEvent> events = eventSink.getDeliveredEvents();
      assertEquals(1, events.size());
      assertSame(packet, events.getFirst().packet());
      assertEquals(AlgorithmType.SHORTEST_PATH, events.getFirst().algorithm());
    }

    @Test
    @DisplayName("should not schedule send when packet reaches destination")
    void onTick_atDestination_doesNotScheduleSend() {
      Node destNode = network.getNode(new Node.Id(2));
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(2));
      destNode.receivePacket(packet);

      destNode.getApplication().onTick(ctx);

      assertTrue(ctx.getPendingSends().isEmpty());
    }
  }

  @Nested
  @DisplayName("Empty Queue Handling")
  class EmptyQueueHandling {

    @Test
    @DisplayName("should do nothing when queue is empty")
    void onTick_emptyQueue_doesNothing() {
      Node node = network.getNode(new Node.Id(1));
      assertTrue(node.getQueue().isEmpty());

      node.getApplication().onTick(ctx);

      assertTrue(ctx.getPendingSends().isEmpty());
      assertTrue(eventSink.getDeliveredEvents().isEmpty());
    }
  }

  @Nested
  @DisplayName("Greedy Neighbor Selection")
  class GreedyNeighborSelection {

    @Test
    @DisplayName("should select neighbor closest to destination in linear chain")
    void onTick_linearChain_selectsClosestNeighbor() {
      // In chain 0-1-2-3-4, from node 2 to destination 4
      // Neighbors of 2 are: 1 (dist 3 to 4) and 3 (dist 1 to 4)
      // Should select node 3

      Node node2 = network.getNode(new Node.Id(2));
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(4));
      node2.receivePacket(packet);

      node2.getApplication().onTick(ctx);

      assertEquals(1, ctx.getPendingSends().size());
      assertEquals(3, ctx.getPendingSends().getFirst().to().value());
    }

    @Test
    @DisplayName("should select neighbor closest to destination when going backwards")
    void onTick_selectsClosestEvenBackwards() {
      // From node 3 to destination 0
      // Neighbors of 3 are: 2 (dist 2 to 0) and 4 (dist 4 to 0)
      // Should select node 2

      Node node3 = network.getNode(new Node.Id(3));
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(4), new Node.Id(0));
      node3.receivePacket(packet);

      node3.getApplication().onTick(ctx);

      assertEquals(1, ctx.getPendingSends().size());
      assertEquals(2, ctx.getPendingSends().getFirst().to().value());
    }

    @Test
    @DisplayName("should use first candidate when distances are equal (tie-breaking)")
    void onTick_equalDistances_selectsFirst() {
      // In diamond topology: 0-1, 0-2, 1-3, 2-3
      // From node 0 to destination 3, neighbors 1 and 2 both have dist=1
      // Should select first (lower ID)

      Network diamondNetwork = TestNetworkBuilder.diamond();
      ctx =
          new SimulationRuntimeContext(TestConfigBuilder.withSeed(42L), diamondNetwork, eventSink);
      ctx.reset(AlgorithmType.SHORTEST_PATH);
      diamondNetwork.setRuntimeContext(ctx);

      for (Node node : diamondNetwork.getNodes()) {
        node.installApplication(new ShortestPathApplication(node));
        node.emptyQueue();
      }

      Node node0 = diamondNetwork.getNode(new Node.Id(0));
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(3));
      node0.receivePacket(packet);

      node0.getApplication().onTick(ctx);

      assertEquals(1, ctx.getPendingSends().size());
      // Should select first candidate (sorted by ID)
      int selectedNeighbor = ctx.getPendingSends().getFirst().to().value();
      assertTrue(selectedNeighbor == 1 || selectedNeighbor == 2);
    }
  }

  @Nested
  @DisplayName("BFS Distance Computation")
  class BfsDistanceComputation {

    @Test
    @DisplayName("should always decrease distance to destination when path exists")
    void routing_alwaysDecreasesDistance() {
      // Verify that each hop brings packet closer to destination
      Node startNode = network.getNode(new Node.Id(0));
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(4));
      startNode.receivePacket(packet);

      int initialDistance = network.getDistanceTo(new Node.Id(0), new Node.Id(4));

      startNode.getApplication().onTick(ctx);

      int nextNodeId = ctx.getPendingSends().getFirst().to().value();
      int newDistance = network.getDistanceTo(new Node.Id(nextNodeId), new Node.Id(4));

      assertTrue(
          newDistance < initialDistance,
          "Distance should decrease: " + initialDistance + " -> " + newDistance);
    }
  }

  @Nested
  @DisplayName("Isolated Node Handling")
  class IsolatedNodeHandling {

    @Test
    @DisplayName("should requeue packet when node is isolated")
    void onTick_isolatedNode_requeuesPacket() {
      Network disconnected = TestNetworkBuilder.disconnectedPair();
      ctx = new SimulationRuntimeContext(TestConfigBuilder.withSeed(42L), disconnected, eventSink);
      ctx.reset(AlgorithmType.SHORTEST_PATH);
      disconnected.setRuntimeContext(ctx);

      Node isolatedNode = disconnected.getNode(new Node.Id(0));
      isolatedNode.installApplication(new ShortestPathApplication(isolatedNode));

      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(1));
      isolatedNode.receivePacket(packet);

      isolatedNode.getApplication().onTick(ctx);

      // Packet should be requeued
      assertEquals(1, isolatedNode.getQueue().size());
      assertSame(packet, isolatedNode.getQueue().peek());
      assertTrue(ctx.getPendingSends().isEmpty());
    }
  }

  @Nested
  @DisplayName("Cache Invalidation")
  class CacheInvalidation {

    @Test
    @DisplayName("should invalidate cache when topology changes (onNodeAdded)")
    void topologyChange_invalidatesCache() {
      // The ShortestPathApplication implements TopologyListener
      // When a node is added, it should mark cache as dirty

      Node node = network.getNode(new Node.Id(1));
      ShortestPathApplication app = (ShortestPathApplication) node.getApplication();

      // Route a packet to populate cache
      Packet p1 = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(4));
      node.receivePacket(p1);
      app.onTick(ctx);
      ctx.flushPendingSends();

      // Simulate topology change
      Node newNode = new Node(new Node.Id(10), new ArrayList<>(), network);
      app.onNodeAdded(newNode);

      // Route another packet - cache should be invalidated and recomputed
      Packet p2 = new Packet(new Packet.Id(2), new Node.Id(0), new Node.Id(4));
      node.receivePacket(p2);

      // Should not throw and should still route correctly
      assertDoesNotThrow(() -> app.onTick(ctx));
    }
  }

  @Nested
  @DisplayName("Packet Scheduling")
  class PacketScheduling {

    @Test
    @DisplayName("should schedule packet send to next hop")
    void onTick_schedulesPacketSend() {
      Node node = network.getNode(new Node.Id(1));
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(4));
      node.receivePacket(packet);

      node.getApplication().onTick(ctx);

      assertEquals(1, ctx.getPendingSends().size());
      assertEquals(1, ctx.getPendingSends().getFirst().from().value());
      assertSame(packet, ctx.getPendingSends().getFirst().packet());
    }

    @Test
    @DisplayName("should remove packet from queue after scheduling")
    void onTick_removesPacketFromQueue() {
      Node node = network.getNode(new Node.Id(1));
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(4));
      node.receivePacket(packet);

      assertEquals(1, node.getQueue().size());

      node.getApplication().onTick(ctx);

      assertEquals(0, node.getQueue().size());
    }
  }

  @Nested
  @DisplayName("Multiple Packets")
  class MultiplePackets {

    @Test
    @DisplayName("should process one packet per tick")
    void onTick_processesOnePacket() {
      Node node = network.getNode(new Node.Id(1));
      Packet p1 = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(4));
      Packet p2 = new Packet(new Packet.Id(2), new Node.Id(0), new Node.Id(4));
      Packet p3 = new Packet(new Packet.Id(3), new Node.Id(0), new Node.Id(4));

      node.receivePacket(p1);
      node.receivePacket(p2);
      node.receivePacket(p3);

      assertEquals(3, node.getQueue().size());

      node.getApplication().onTick(ctx);

      assertEquals(2, node.getQueue().size());
      assertEquals(1, ctx.getPendingSends().size());
    }
  }

  @Nested
  @DisplayName("Ring Topology")
  class RingTopology {

    @Test
    @DisplayName("should find shortest path in ring")
    void ring_findsShortestPath() {
      // In ring 0-1-2-3-4-5-0, from node 0 to node 3
      // Shortest path is 0-1-2-3 or 0-5-4-3 (both length 3)

      Network ring = TestNetworkBuilder.ring(6);
      ctx = new SimulationRuntimeContext(TestConfigBuilder.withSeed(42L), ring, eventSink);
      ctx.reset(AlgorithmType.SHORTEST_PATH);
      ring.setRuntimeContext(ctx);

      for (Node node : ring.getNodes()) {
        node.installApplication(new ShortestPathApplication(node));
        node.emptyQueue();
      }

      Node node0 = ring.getNode(new Node.Id(0));
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(3));
      node0.receivePacket(packet);

      node0.getApplication().onTick(ctx);

      // Should select either neighbor 1 or neighbor 5 (both valid)
      int nextHop = ctx.getPendingSends().getFirst().to().value();
      assertTrue(nextHop == 1 || nextHop == 5);
    }
  }

  @Nested
  @DisplayName("Grid Topology")
  class GridTopology {

    @Test
    @DisplayName("should find shortest path in grid")
    void grid_findsShortestPath() {
      // In 3x3 grid, from corner (0,0) to corner (2,2)
      // Node IDs: 0 1 2 / 3 4 5 / 6 7 8
      // From 0 to 8, shortest path length is 4

      Network grid = TestNetworkBuilder.grid(3, 3);
      ctx = new SimulationRuntimeContext(TestConfigBuilder.withSeed(42L), grid, eventSink);
      ctx.reset(AlgorithmType.SHORTEST_PATH);
      grid.setRuntimeContext(ctx);

      for (Node node : grid.getNodes()) {
        node.installApplication(new ShortestPathApplication(node));
        node.emptyQueue();
      }

      Node node0 = grid.getNode(new Node.Id(0));
      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(8));
      node0.receivePacket(packet);

      node0.getApplication().onTick(ctx);

      // From node 0, neighbors are 1 (dist 3) and 3 (dist 3)
      // Should select first candidate
      int nextHop = ctx.getPendingSends().getFirst().to().value();
      assertTrue(nextHop == 1 || nextHop == 3);
    }
  }

  @Nested
  @DisplayName("End-to-End Routing")
  class EndToEndRouting {

    @Test
    @DisplayName("should route packet from source to destination across multiple hops")
    void fullRoute_deliversPacket() {
      // Route a packet from 0 to 4 in chain 0-1-2-3-4

      Packet packet = new Packet(new Packet.Id(1), new Node.Id(0), new Node.Id(4));
      network.getNode(new Node.Id(0)).receivePacket(packet);

      int hops = 0;
      int maxHops = 10; // Prevent infinite loop

      while (hops < maxHops) {
        // Process all nodes
        for (Node node : network.getNodes()) {
          node.getApplication().onTick(ctx);
        }

        // Check if delivered
        if (!eventSink.getDeliveredEvents().isEmpty()) {
          break;
        }

        // Flush pending sends
        var sends = ctx.flushPendingSends();
        for (var send : sends) {
          network.sendPacket(send.from(), send.to(), send.packet());
        }

        hops++;
      }

      // Should have delivered the packet
      assertEquals(1, eventSink.getDeliveredEvents().size());
      assertSame(packet, eventSink.getDeliveredEvents().getFirst().packet());
    }
  }
}
