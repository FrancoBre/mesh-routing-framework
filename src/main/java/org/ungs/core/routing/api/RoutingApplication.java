package org.ungs.core.routing.api;

import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ungs.core.engine.Scheduler;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Node;
import org.ungs.core.network.Packet;

@Getter
@RequiredArgsConstructor
public abstract class RoutingApplication {

  private final Scheduler scheduler;
  private final Node node;

  public abstract void onTick(SimulationRuntimeContext ctx);

  public Node.Id getNodeId() {
    return node.getId();
  }

  public Optional<Packet> getNextPacket() {
    return Optional.ofNullable(node.getNextPacket());
  }

  public abstract AlgorithmType getType();
}
