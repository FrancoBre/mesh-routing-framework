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
    return new Network(this.createIrregular6x6Grid());
  }

  private List<Node> createIrregular6x6Grid() {
    List<Node> nodes = new ArrayList<>();
    // Initialize nodes
    for (int i = 0; i < 36; i++) {
      nodes.add(new Node(new Node.Id(i), new ArrayList<>()));
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
    remove(nodes, 14, 20);
    remove(nodes, 15, 21);

    return nodes;
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
