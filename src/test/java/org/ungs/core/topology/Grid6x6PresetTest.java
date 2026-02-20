package org.ungs.core.topology;

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
import org.ungs.core.topology.api.TopologyType;
import org.ungs.core.topology.presets.Grid6x6Preset;

@DisplayName("Grid6x6Preset")
class Grid6x6PresetTest {

  private Grid6x6Preset preset;
  private Network network;

  @BeforeEach
  void setUp() {
    preset = new Grid6x6Preset();
    network = preset.createNetwork();
  }

  @Nested
  @DisplayName("Node Creation")
  class NodeCreation {

    @Test
    @DisplayName("should create exactly 36 nodes")
    void createsThirtySixNodes() {
      assertEquals(36, network.getNodes().size());
    }

    @Test
    @DisplayName("should create nodes with IDs 0 through 35")
    void nodesHaveCorrectIds() {
      List<Node> nodes = network.getNodes();
      Set<Integer> ids = new HashSet<>();

      for (Node node : nodes) {
        ids.add(node.getId().value());
      }

      assertEquals(36, ids.size());
      for (int i = 0; i < 36; i++) {
        assertTrue(ids.contains(i), "Missing node ID: " + i);
      }
    }

    @Test
    @DisplayName("nodes should be sorted by ID")
    void nodesAreSortedById() {
      List<Node> nodes = network.getNodes();
      for (int i = 0; i < nodes.size() - 1; i++) {
        assertTrue(nodes.get(i).getId().value() < nodes.get(i + 1).getId().value());
      }
    }
  }

  @Nested
  @DisplayName("Basic Grid Connections")
  class BasicGridConnections {

    @Test
    @DisplayName("corner node 0 should have 2 neighbors (right and down)")
    void cornerNodeZero_hasTwoNeighbors() {
      Node node0 = network.getNode(new Node.Id(0));
      // Node 0 is top-left corner
      // In regular grid: neighbors are 1 (right) and 6 (down)
      // But some edges might be removed for holes

      // According to the preset, node 0 should still have neighbors
      assertTrue(!node0.getNeighbors().isEmpty() && node0.getNeighbors().size() <= 2);
    }

    @Test
    @DisplayName("corner node 35 should have appropriate neighbors")
    void cornerNode35_hasNeighbors() {
      Node node35 = network.getNode(new Node.Id(35));
      // Node 35 is bottom-right corner
      // In regular grid: neighbors are 34 (left) and 29 (up)

      assertTrue(!node35.getNeighbors().isEmpty() && node35.getNeighbors().size() <= 2);
    }

    @Test
    @DisplayName("all connections should be bidirectional")
    void connectionsBidirectional() {
      for (Node node : network.getNodes()) {
        for (Node neighbor : node.getNeighbors()) {
          assertTrue(
              neighbor.getNeighbors().contains(node),
              "Connection from "
                  + node.getId()
                  + " to "
                  + neighbor.getId()
                  + " is not bidirectional");
        }
      }
    }
  }

  @Nested
  @DisplayName("Irregular Edges (Holes)")
  class IrregularEdges {

    @Test
    @DisplayName("node 8 and 9 should not be neighbors (hole)")
    void hole_8_9() {
      Node node8 = network.getNode(new Node.Id(8));
      Node node9 = network.getNode(new Node.Id(9));

      assertFalse(node8.getNeighbors().contains(node9));
      assertFalse(node9.getNeighbors().contains(node8));
    }

    @Test
    @DisplayName("node 20 and 21 should not be neighbors (hole)")
    void hole_20_21() {
      Node node20 = network.getNode(new Node.Id(20));
      Node node21 = network.getNode(new Node.Id(21));

      assertFalse(node20.getNeighbors().contains(node21));
      assertFalse(node21.getNeighbors().contains(node20));
    }

    @Test
    @DisplayName("node 26 and 27 should not be neighbors (hole)")
    void hole_26_27() {
      Node node26 = network.getNode(new Node.Id(26));
      Node node27 = network.getNode(new Node.Id(27));

      assertFalse(node26.getNeighbors().contains(node27));
      assertFalse(node27.getNeighbors().contains(node26));
    }

    @Test
    @DisplayName("node 1 and 7 should not be neighbors (hole)")
    void hole_1_7() {
      Node node1 = network.getNode(new Node.Id(1));
      Node node7 = network.getNode(new Node.Id(7));

      assertFalse(node1.getNeighbors().contains(node7));
      assertFalse(node7.getNeighbors().contains(node1));
    }

    @Test
    @DisplayName("node 6 and 7 should not be neighbors (hole)")
    void hole_6_7() {
      Node node6 = network.getNode(new Node.Id(6));
      Node node7 = network.getNode(new Node.Id(7));

      assertFalse(node6.getNeighbors().contains(node7));
      assertFalse(node7.getNeighbors().contains(node6));
    }

    @Test
    @DisplayName("node 2 and 8 should not be neighbors (hole)")
    void hole_2_8() {
      Node node2 = network.getNode(new Node.Id(2));
      Node node8 = network.getNode(new Node.Id(8));

      assertFalse(node2.getNeighbors().contains(node8));
      assertFalse(node8.getNeighbors().contains(node2));
    }

    @Test
    @DisplayName("node 3 and 9 should not be neighbors (hole)")
    void hole_3_9() {
      Node node3 = network.getNode(new Node.Id(3));
      Node node9 = network.getNode(new Node.Id(9));

      assertFalse(node3.getNeighbors().contains(node9));
      assertFalse(node9.getNeighbors().contains(node3));
    }

    @Test
    @DisplayName("node 4 and 10 should not be neighbors (hole)")
    void hole_4_10() {
      Node node4 = network.getNode(new Node.Id(4));
      Node node10 = network.getNode(new Node.Id(10));

      assertFalse(node4.getNeighbors().contains(node10));
      assertFalse(node10.getNeighbors().contains(node4));
    }

    @Test
    @DisplayName("node 10 and 11 should not be neighbors (hole)")
    void hole_10_11() {
      Node node10 = network.getNode(new Node.Id(10));
      Node node11 = network.getNode(new Node.Id(11));

      assertFalse(node10.getNeighbors().contains(node11));
      assertFalse(node11.getNeighbors().contains(node10));
    }

    @Test
    @DisplayName("node 32 and 33 should not be neighbors (hole)")
    void hole_32_33() {
      Node node32 = network.getNode(new Node.Id(32));
      Node node33 = network.getNode(new Node.Id(33));

      assertFalse(node32.getNeighbors().contains(node33));
      assertFalse(node33.getNeighbors().contains(node32));
    }
  }

  @Nested
  @DisplayName("Regular Grid Connections (Not Removed)")
  class RegularConnections {

    @Test
    @DisplayName("node 0 and 1 should be neighbors")
    void connected_0_1() {
      Node node0 = network.getNode(new Node.Id(0));
      Node node1 = network.getNode(new Node.Id(1));

      assertTrue(node0.getNeighbors().contains(node1));
      assertTrue(node1.getNeighbors().contains(node0));
    }

    @Test
    @DisplayName("node 0 and 6 should be neighbors")
    void connected_0_6() {
      Node node0 = network.getNode(new Node.Id(0));
      Node node6 = network.getNode(new Node.Id(6));

      assertTrue(node0.getNeighbors().contains(node6));
      assertTrue(node6.getNeighbors().contains(node0));
    }

    @Test
    @DisplayName("center node 14 should have neighbors")
    void centerNode_hasNeighbors() {
      Node node14 = network.getNode(new Node.Id(14));
      // Center-ish node should have neighbors on multiple sides
      assertFalse(node14.getNeighbors().isEmpty());
    }
  }

  @Nested
  @DisplayName("Network Connectivity")
  class NetworkConnectivity {

    @Test
    @DisplayName("network should be connected (all nodes reachable from node 0)")
    void networkIsConnected() {
      // BFS from node 0 should reach all nodes
      Set<Integer> visited = new HashSet<>();
      java.util.Queue<Node> queue = new java.util.LinkedList<>();

      queue.add(network.getNode(new Node.Id(0)));
      visited.add(0);

      while (!queue.isEmpty()) {
        Node current = queue.poll();
        for (Node neighbor : current.getNeighbors()) {
          if (!visited.contains(neighbor.getId().value())) {
            visited.add(neighbor.getId().value());
            queue.add(neighbor);
          }
        }
      }

      assertEquals(36, visited.size(), "Not all nodes are reachable from node 0");
    }

    @Test
    @DisplayName("should be able to compute distance between any two nodes")
    void canComputeDistances() {
      // Test distance computation on a few pairs
      int dist0_35 = network.getDistanceTo(new Node.Id(0), new Node.Id(35));
      assertTrue(dist0_35 < Integer.MAX_VALUE, "Node 35 should be reachable from node 0");
      assertTrue(dist0_35 > 0, "Distance should be positive");

      int dist15_20 = network.getDistanceTo(new Node.Id(15), new Node.Id(20));
      assertTrue(dist15_20 < Integer.MAX_VALUE, "Node 20 should be reachable from node 15");
    }
  }

  @Nested
  @DisplayName("Preset Metadata")
  class PresetMetadata {

    @Test
    @DisplayName("should return correct topology type")
    void type_returns6x6Grid() {
      assertEquals(TopologyType._6X6_GRID, preset.type());
    }
  }
}
