package org.ungs.core;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ToString
@RequiredArgsConstructor
class Network {

  private final Registry registry = Registry.getInstance();

  private final List<Node> nodes;

  Node getNode(Node.Id origin) {
    return nodes.stream()
        .filter(node -> node.getId().equals(origin))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Node not found: " + origin));
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

    registry.registerHop(packet, from, to, Simulation.TIME, Simulation.TIME++);
    receiverNode.receivePacket(packet);
  }
}
