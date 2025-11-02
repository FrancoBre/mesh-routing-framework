package org.ungs.routing.qrouting;

import lombok.extern.slf4j.Slf4j;
import org.ungs.core.Node;
import org.ungs.core.Packet;
import org.ungs.core.Registry;
import org.ungs.core.Scheduler;
import org.ungs.core.Simulation;
import org.ungs.routing.RoutingApplication;

@Slf4j
public class QRoutingApplication extends RoutingApplication {

  public QRoutingApplication(Node node) {
    super(Registry.getInstance(), Scheduler.getInstance(), node);
  }

  @Override
  public void onTick() {
    log.debug("[nodeId={}, time={}]: Q-Routing tick", this.getNodeId(), Simulation.TIME);

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

    var nextHop = this.selectNextHop(packetToProcess);

    this.getScheduler().schedule(this.getNodeId(), nextHop, packetToProcess);
    log.info(
        "[time={}]: Packet {} forwarded from Node {} to Node {}",
        Simulation.TIME,
        packetToProcess.getId(),
        this.getNodeId(),
        nextHop);
  }

  private Node.Id selectNextHop(Packet packetToProcess) {
    var neighbors = this.getNode().getNeighbors();

    // TODO
    // Placeholder logic: randomly select a neighbor as the next hop
    var randomIndex = (int) (Math.random() * neighbors.size());
    return neighbors.get(randomIndex).getId();
  }
}
