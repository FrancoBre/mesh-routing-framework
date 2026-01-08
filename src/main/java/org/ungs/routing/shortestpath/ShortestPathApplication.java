package org.ungs.routing.shortestpath;

import static org.ungs.core.Simulation.RANDOM;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import lombok.extern.slf4j.Slf4j;
import org.ungs.core.Node;
import org.ungs.core.Registry;
import org.ungs.core.Scheduler;
import org.ungs.core.Simulation;
import org.ungs.core.TopologyListener;
import org.ungs.routing.RoutingApplication;

@Slf4j
public class ShortestPathApplication extends RoutingApplication implements TopologyListener {

  private final Map<Node.Id, Map<Node.Id, Integer>> distToDestCache = new HashMap<>();
  private boolean dirtyDistToDestCache = false;

  public ShortestPathApplication(Node node) {
    super(Registry.getInstance(), Scheduler.getInstance(), node);
    node.getNetwork().addTopologyListener(this);
  }

  @Override
  public void onTick() {
    // if (this.getNodeId().value() == 2) {
    //   if (Simulation.TIME % 2 != 0) {
    //     log.debug(
    //         "[onTick] Time={} - NodeId={} is throttled this tick",
    //         Simulation.TIME,
    //         this.getNodeId());
    //     return;
    //   }
    // }

    var packetToProcessOrEmpty = this.getNextPacket();

    if (packetToProcessOrEmpty.isEmpty()) {
      return;
    }

    var packetToProcess = packetToProcessOrEmpty.get();

    if (this.getNodeId().equals(packetToProcess.getDestination())) {
      log.info(
          "[nodeId={}, time={}]: Packet {} has reached its destination",
          this.getNodeId(),
          Simulation.TIME,
          packetToProcess.getId());
      this.getRegistry().registerReceivedPacket(packetToProcess);
      return;
    }

    var neighbors = new ArrayList<>(this.getNode().getNeighbors());
    neighbors.sort(Comparator.comparing(n -> n.getId().value()));

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
      bestNextNode = neighbors.get(RANDOM.nextIndex(neighbors.size()));
    } else {
      bestNextNode = bestCandidates.get(0);
    }

    log.debug("[onTick] Time={} - NodeId={}", Simulation.TIME, this.getNodeId());
    log.debug(
        "[onTick] Time={} - NodeId={} - Chose next node {} for packet {}",
        Simulation.TIME,
        this.getNodeId(),
        bestNextNode.getId(),
        packetToProcess.getId());

    this.getScheduler().schedule(this.getNodeId(), bestNextNode.getId(), packetToProcess);
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
