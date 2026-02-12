package org.ungs.core.dynamics.impl;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.ungs.core.config.NetworkDynamicsConfig.ScheduledLinkFailures;
import org.ungs.core.config.NetworkDynamicsConfig.ScheduledLinkFailures.LinkSpec;
import org.ungs.core.dynamics.api.NetworkDynamics;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Network;
import org.ungs.core.network.Node;
import org.ungs.core.observability.api.SimulationObserver;

/**
 * Implements scheduled link disconnection/reconnection dynamics.
 *
 * <p>This simulates the experiment described in Boyan & Littman (1993) where links are manually
 * disconnected during simulation to test Q-routing's ability to adapt to topology changes.
 *
 * <p>Links are disconnected at a specified tick and optionally reconnected at a later tick.
 */
@Slf4j
public final class ScheduledLinkFailuresDynamics implements NetworkDynamics, SimulationObserver {

  private final int disconnectAtTick;
  private final int reconnectAtTick;
  private final List<LinkSpec> links;

  private boolean disconnected = false;
  private boolean reconnected = false;

  public ScheduledLinkFailuresDynamics(ScheduledLinkFailures cfg) {
    this.disconnectAtTick = cfg.disconnectAtTick();
    this.reconnectAtTick = cfg.reconnectAtTick();
    this.links = cfg.links();
  }

  @Override
  public void onAlgorithmEnd(SimulationRuntimeContext ctx) {
    // Restore links if they were disconnected during this algorithm run
    if (disconnected) {
      Network network = ctx.getNetwork();
      for (LinkSpec link : links) {
        reconnectLink(network, link.nodeA(), link.nodeB());
      }
      log.info(
          "[Algorithm End] Restored {} link(s) for next algorithm run: {}",
          links.size(),
          formatLinks(links));
    }
    // Reset internal state for next algorithm
    disconnected = false;
    reconnected = false;
  }

  @Override
  public void beforeTick(SimulationRuntimeContext ctx) {
    int currentTick = (int) ctx.getTick();
    Network network = ctx.getNetwork();

    // Disconnect links at the specified tick
    if (!disconnected && currentTick >= disconnectAtTick) {
      for (LinkSpec link : links) {
        disconnectLink(network, link.nodeA(), link.nodeB());
      }
      disconnected = true;
      log.info(
          "[Tick {}] Disconnected {} link(s): {}",
          currentTick,
          links.size(),
          formatLinks(links));
    }

    // Reconnect links at the specified tick (if configured)
    if (disconnected && !reconnected && reconnectAtTick > 0 && currentTick >= reconnectAtTick) {
      for (LinkSpec link : links) {
        reconnectLink(network, link.nodeA(), link.nodeB());
      }
      reconnected = true;
      log.info(
          "[Tick {}] Reconnected {} link(s): {}",
          currentTick,
          links.size(),
          formatLinks(links));
    }
  }

  private void disconnectLink(Network network, int nodeAId, int nodeBId) {
    Node nodeA = network.getNode(new Node.Id(nodeAId));
    Node nodeB = network.getNode(new Node.Id(nodeBId));

    boolean removedFromA = nodeA.getNeighbors().remove(nodeB);
    boolean removedFromB = nodeB.getNeighbors().remove(nodeA);

    if (!removedFromA || !removedFromB) {
      log.warn(
          "Link {}-{} was not fully connected (A->B: {}, B->A: {})",
          nodeAId,
          nodeBId,
          removedFromA,
          removedFromB);
    }
  }

  private void reconnectLink(Network network, int nodeAId, int nodeBId) {
    Node nodeA = network.getNode(new Node.Id(nodeAId));
    Node nodeB = network.getNode(new Node.Id(nodeBId));

    // Only add if not already neighbors (avoid duplicates)
    if (!nodeA.getNeighbors().contains(nodeB)) {
      nodeA.getNeighbors().add(nodeB);
    }
    if (!nodeB.getNeighbors().contains(nodeA)) {
      nodeB.getNeighbors().add(nodeA);
    }
  }

  private String formatLinks(List<LinkSpec> links) {
    return links.stream()
        .map(l -> l.nodeA() + "-" + l.nodeB())
        .reduce((a, b) -> a + ", " + b)
        .orElse("");
  }
}
