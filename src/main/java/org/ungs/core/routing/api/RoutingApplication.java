package org.ungs.core.routing.api;

import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Node;
import org.ungs.core.network.Packet;

@Getter
@RequiredArgsConstructor
public abstract class RoutingApplication {

  private final Node node;

  /**
   * Called once per tick BEFORE any node processes packets. Q-learning algorithms override this to
   * snapshot their Q-tables, ensuring all nodes see start-of-tick values when querying neighbors.
   */
  public void onTickStart(SimulationRuntimeContext ctx) {
    // default no-op
  }

  public abstract void onTick(SimulationRuntimeContext ctx);

  public Node.Id getNodeId() {
    return node.getId();
  }

  public Optional<Packet> getNextPacket() {
    return Optional.ofNullable(node.getNextPacket());
  }

  public abstract AlgorithmType getType();
}
