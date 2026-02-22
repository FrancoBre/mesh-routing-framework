package org.ungs.core.network;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.observability.events.HopEvent;
import org.ungs.core.topology.api.TopologyListener;

@Slf4j
@ToString
public class Network {

  private final List<Node> nodes;

  private final List<TopologyListener> listeners = new ArrayList<>();

  @Getter @Setter private SimulationRuntimeContext ctx;

  public void setRuntimeContext(SimulationRuntimeContext ctx) {
    this.ctx = ctx;
  }

  public Network() {
    this.nodes = new ArrayList<>();
  }

  public void addTopologyListener(TopologyListener l) {
    listeners.add(l);
  }

  public void addNode(Node node) {
    this.nodes.add(node);
    this.nodes.sort(Comparator.comparing(n -> n.getId().value()));

    for (TopologyListener l : listeners) {
      l.onNodeAdded(node);
    }
  }

  public Node getNode(Node.Id nodeId) {
    return nodes.stream()
        .filter(node -> node.getId().equals(nodeId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Node not found: " + nodeId));
  }

  public List<Node> getNodes() {
    return List.copyOf(this.nodes);
  }

  public void sendPacket(Node.Id from, Node.Id to, Packet packet) {
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
        ctx.getTick(),
        packet.getId().value(),
        from.value(),
        to.value());

    ctx.getEventSink()
        .emit(
            new HopEvent(
                packet.getId(),
                from,
                to,
                (long) ctx.getTick(),
                (long) (ctx.getTick() + 1),
                ctx.getCurrentAlgorithm()));

    packet.resetTimeInQueue();
    receiverNode.receivePacket(packet);
  }

  public int getDistanceTo(Node.Id from, Node.Id destination) {
    if (from.equals(destination)) {
      return 0;
    }

    Map<Node.Id, Integer> dist = new HashMap<>();
    ArrayDeque<Node> q = new ArrayDeque<>();

    Node fromNode = getNode(from);
    dist.put(from, 0);
    q.add(fromNode);

    while (!q.isEmpty()) {
      Node cur = q.poll();
      int curDist = dist.get(cur.getId());

      for (Node nb : cur.getNeighbors()) {
        Node.Id nbId = nb.getId();
        if (!dist.containsKey(nbId)) {
          dist.put(nbId, curDist + 1);
          if (nbId.equals(destination)) {
            return curDist + 1;
          }
          q.add(nb);
        }
      }
    }

    // No path found
    return Integer.MAX_VALUE;
  }

  public int packetsInFlight() {
    return nodes.stream().mapToInt(node -> node.getQueue().size()).sum();
  }

  public boolean isNeighbor(Node.Id a, Node.Id b) {
    Node nodeA = getNode(a);
    Node nodeB = getNode(b);
    return nodeA.getNeighbors().contains(nodeB);
  }
}
