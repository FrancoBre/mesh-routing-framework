package org.ungs.core.routing.impl.shortestpath;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import lombok.extern.slf4j.Slf4j;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Node;
import org.ungs.core.observability.events.PacketDeliveredEvent;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.core.routing.api.RoutingApplication;
import org.ungs.core.topology.api.TopologyListener;

@Slf4j
public class ShortestPathApplication extends RoutingApplication implements TopologyListener {

  private final Map<Node.Id, Map<Node.Id, Integer>> distToDestCache = new HashMap<>();
  private boolean dirtyDistToDestCache = false;

  public ShortestPathApplication(Node node) {
    super(node);
    node.getNetwork().addTopologyListener(this);
  }

  public AlgorithmType getType() {
    return AlgorithmType.SHORTEST_PATH;
  }

  @Override
  public void onTick(SimulationRuntimeContext ctx) {
    var packetToProcessOrEmpty = this.getNextPacket();

    if (packetToProcessOrEmpty.isEmpty()) {
      return;
    }

    var packetToProcess = packetToProcessOrEmpty.get();

    if (this.getNodeId().equals(packetToProcess.getDestination())) {
      log.info(
          "[nodeId={}, time={}]: Packet {} has reached its destination",
          this.getNodeId(),
          ctx.getTick(),
          packetToProcess.getId());

      ctx.getEventSink()
          .emit(new PacketDeliveredEvent(packetToProcess, 0, ctx.getTick(), this.getType()));
      return;
    }

    var neighbors = new ArrayList<>(this.getNode().getNeighbors());
    neighbors.sort(Comparator.comparing(n -> n.getId().value()));

    // If node is isolated (no neighbors), return packet to queue to wait for reconnection
    if (neighbors.isEmpty()) {
      log.warn(
          "[nodeId={}, time={}]: Node is isolated - packet {} returned to queue",
          this.getNodeId(),
          ctx.getTick(),
          packetToProcess.getId());
      this.getNode().getQueue().addFirst(packetToProcess);
      return;
    }

    int bestDist = Integer.MAX_VALUE;
    List<Node> bestCandidates = new ArrayList<>();

    for (Node nb : neighbors) {
      int d = this.getDistanceToDestination(nb.getId(), packetToProcess.getDestination());

      if (d < bestDist) {
        bestDist = d;
        bestCandidates.clear();
        bestCandidates.add(nb);
      } else if (d == bestDist) {
        bestCandidates.add(nb);
      }
    }

    Node bestNextNode;
    if (bestCandidates.isEmpty() || bestDist == Integer.MAX_VALUE) {
      bestNextNode = neighbors.get(ctx.getRng().nextIndex(neighbors.size()));
    } else {
      bestNextNode = bestCandidates.getFirst();
    }

    log.debug("[onTick] Time={} - NodeId={}", ctx.getTick(), this.getNodeId());
    log.debug(
        "[onTick] Time={} - NodeId={} - Chose next node {} for packet {}",
        ctx.getTick(),
        this.getNodeId(),
        bestNextNode.getId(),
        packetToProcess.getId());

    ctx.schedule(this.getNodeId(), bestNextNode.getId(), packetToProcess);
  }

  @Override
  public void onNodeAdded(Node node) {
    log.debug("[ShortestPath] Topology changed, recomputing distances at node {}", getNodeId());

    this.dirtyDistToDestCache = true;
  }

  private int getDistanceToDestination(Node.Id from, Node.Id to) {
    if (dirtyDistToDestCache) {
      distToDestCache.put(to, computeDistancesToDestination(to));
      dirtyDistToDestCache = false;
    }

    return distToDestCache
        .computeIfAbsent(to, this::computeDistancesToDestination)
        .getOrDefault(from, Integer.MAX_VALUE);
  }

  /**
   * In the current simulator configuration, the delivery time of a hop is constant and equal to one
   * tick. Therefore, the shortest path in terms of total delivery time coincides with the shortest
   * path in terms of hop count. This assumption can be relaxed in future extensions where link
   * delays depend on physical or network-level parameters.
   */
  private Map<Node.Id, Integer> computeDistancesToDestination(Node.Id destination) {
    Map<Node.Id, Integer> dist = new HashMap<>();
    Queue<Node.Id> queue = new ArrayDeque<>();

    dist.put(destination, 0);
    queue.add(destination);

    while (!queue.isEmpty()) {
      Node.Id current = queue.poll();
      int currentDist = dist.get(current);

      Node currentNode = getNode().getNetwork().getNode(current);

      for (Node neighbor : currentNode.getNeighbors()) {
        Node.Id nid = neighbor.getId();
        if (!dist.containsKey(nid)) {
          dist.put(nid, currentDist + 1);
          queue.add(nid);
        }
      }
    }

    return dist;
  }
}
