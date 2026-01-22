package org.ungs.core;

import java.util.ArrayList;
import java.util.List;

public final class Grid6x6Preset implements TopologyPreset {

  @Override
  public TopologyType type() {
    return TopologyType.GRID_6X6;
  }

  @Override
  public Network createNetwork() {
    var network = new Network();

    List<Node> nodes = new ArrayList<>();
    // Initialize nodes
    for (int i = 0; i < 36; i++) {
      nodes.add(new Node(new Node.Id(i), new ArrayList<>(), network));
    }

    // Connect neighbors in a regular grid
    for (int i = 0; i < 36; i++) {
      int row = i / 6;
      int col = i % 6;
      if (col < 5) {
        connect(nodes, i, i + 1);
      }
      if (row < 5) {
        connect(nodes, i, i + 6);
      }
    }

    // Remove vertical edges for the 2x2 hole
    remove(nodes, 8, 9);
    remove(nodes, 20, 21);
    remove(nodes, 26, 27);
    remove(nodes, 1, 7);
    remove(nodes, 6, 7);
    remove(nodes, 2, 8);
    remove(nodes, 3, 9);
    remove(nodes, 4, 10);
    remove(nodes, 10, 11);

    remove(nodes, 32, 33);

    nodes.forEach(network::addNode);
    return network;
  }

  private static void connect(List<Node> nodes, int a, int b) {
    nodes.get(a).getNeighbors().add(nodes.get(b));
    nodes.get(b).getNeighbors().add(nodes.get(a));
  }

  private static void remove(List<Node> nodes, int a, int b) {
    nodes.get(a).getNeighbors().remove(nodes.get(b));
    nodes.get(b).getNeighbors().remove(nodes.get(a));
  }
}
