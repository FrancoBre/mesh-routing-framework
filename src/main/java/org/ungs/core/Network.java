package org.ungs.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
class Network {

  private final Registry registry = Registry.getInstance();

  private final List<Node> nodes;

  public Network() {
    this.nodes = new ArrayList<>();
  }

  public void addNode(Node node) {
    this.nodes.add(node);
    this.nodes.sort(Comparator.comparing(n -> n.getId().value()));
  }

  Node getNode(Node.Id nodeId) {
    return nodes.stream()
        .filter(node -> node.getId().equals(nodeId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Node not found: " + nodeId));
  }

  public List<Node> getNodes() {
    return List.copyOf(this.nodes);
  }

  void sendPacket(Node.Id from, Node.Id to, Packet packet) {
    Node senderNode =
        nodes.stream()
            .filter(node -> node.getId().equals(from))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Sender node not found: " + from));

    Node receiverNode =
        nodes.stream()
            .filter(node -> node.getId().equals(to))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Receiver node not found: " + to));

    if (!senderNode.getNeighbors().contains(receiverNode)) {
      throw new IllegalArgumentException("Nodes are not neighbors: " + from + " and " + to);
    }

    log.debug(
        "[time={}] Packet {} sent from Node {} to Node {}",
        Simulation.TIME,
        packet.getId().value(),
        from.value(),
        to.value());

    registry.registerHop(packet, from, to, Simulation.TIME, Simulation.TIME + 1);
    receiverNode.receivePacket(packet);
  }
}
