package org.ungs.testutil;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.ungs.core.network.Network;
import org.ungs.core.network.Node;

@UtilityClass
public final class TestNetworkBuilder {

  /**
   * Creates a linear chain topology: 0 - 1 - 2 - ... - (size-1)
   *
   * @param size Number of nodes in the chain
   * @return Network with linear topology
   */
  public static Network linearChain(int size) {
    if (size < 1) {
      throw new IllegalArgumentException("Chain size must be at least 1");
    }

    Network network = new Network();
    List<Node> nodes = new ArrayList<>();

    for (int i = 0; i < size; i++) {
      nodes.add(new Node(new Node.Id(i), new ArrayList<>(), network));
    }

    // Connect adjacent nodes
    for (int i = 0; i < size - 1; i++) {
      connect(nodes, i, i + 1);
    }

    nodes.forEach(network::addNode);
    return network;
  }

  /**
   * Creates a ring topology: 0 - 1 - 2 - ... - (size-1) - 0
   *
   * @param size Number of nodes in the ring
   * @return Network with ring topology
   */
  public static Network ring(int size) {
    if (size < 3) {
      throw new IllegalArgumentException("Ring size must be at least 3");
    }

    Network network = new Network();
    List<Node> nodes = new ArrayList<>();

    for (int i = 0; i < size; i++) {
      nodes.add(new Node(new Node.Id(i), new ArrayList<>(), network));
    }

    // Connect adjacent nodes
    for (int i = 0; i < size - 1; i++) {
      connect(nodes, i, i + 1);
    }
    // Close the ring
    connect(nodes, size - 1, 0);

    nodes.forEach(network::addNode);
    return network;
  }

  /**
   * Creates a star topology with a central hub node (node 0) connected to all spokes.
   *
   * @param spokes Number of spoke nodes (excluding the hub)
   * @return Network with star topology
   */
  public static Network star(int spokes) {
    if (spokes < 1) {
      throw new IllegalArgumentException("Star must have at least 1 spoke");
    }

    Network network = new Network();
    List<Node> nodes = new ArrayList<>();

    // Hub is node 0
    nodes.add(new Node(new Node.Id(0), new ArrayList<>(), network));

    // Create spokes
    for (int i = 1; i <= spokes; i++) {
      nodes.add(new Node(new Node.Id(i), new ArrayList<>(), network));
    }

    // Connect hub to all spokes
    for (int i = 1; i <= spokes; i++) {
      connect(nodes, 0, i);
    }

    nodes.forEach(network::addNode);
    return network;
  }

  /**
   * Creates a network with two disconnected nodes (no neighbors). Useful for testing isolated node
   * handling.
   *
   * @return Network with two isolated nodes
   */
  public static Network disconnectedPair() {
    Network network = new Network();

    Node node0 = new Node(new Node.Id(0), new ArrayList<>(), network);
    Node node1 = new Node(new Node.Id(1), new ArrayList<>(), network);

    network.addNode(node0);
    network.addNode(node1);

    return network;
  }

  /**
   * Creates a grid topology of specified dimensions. Node IDs are assigned row-major: node at (row,
   * col) has ID = row * cols + col
   *
   * @param rows Number of rows
   * @param cols Number of columns
   * @return Network with grid topology
   */
  public static Network grid(int rows, int cols) {
    if (rows < 1 || cols < 1) {
      throw new IllegalArgumentException("Grid dimensions must be at least 1x1");
    }

    Network network = new Network();
    List<Node> nodes = new ArrayList<>();

    int totalNodes = rows * cols;
    for (int i = 0; i < totalNodes; i++) {
      nodes.add(new Node(new Node.Id(i), new ArrayList<>(), network));
    }

    // Connect horizontal neighbors
    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols - 1; col++) {
        int current = row * cols + col;
        int right = current + 1;
        connect(nodes, current, right);
      }
    }

    // Connect vertical neighbors
    for (int row = 0; row < rows - 1; row++) {
      for (int col = 0; col < cols; col++) {
        int current = row * cols + col;
        int below = current + cols;
        connect(nodes, current, below);
      }
    }

    nodes.forEach(network::addNode);
    return network;
  }

  /**
   * Creates a diamond topology: 1 / \ 0 3 \ / 2
   *
   * @return Network with diamond topology
   */
  public static Network diamond() {
    Network network = new Network();
    List<Node> nodes = new ArrayList<>();

    for (int i = 0; i < 4; i++) {
      nodes.add(new Node(new Node.Id(i), new ArrayList<>(), network));
    }

    connect(nodes, 0, 1);
    connect(nodes, 0, 2);
    connect(nodes, 1, 3);
    connect(nodes, 2, 3);

    nodes.forEach(network::addNode);
    return network;
  }

  private static void connect(List<Node> nodes, int a, int b) {
    nodes.get(a).getNeighbors().add(nodes.get(b));
    nodes.get(b).getNeighbors().add(nodes.get(a));
  }
}
