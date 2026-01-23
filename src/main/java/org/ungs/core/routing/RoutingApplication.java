package org.ungs.core.routing;

import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ungs.core.Node;
import org.ungs.core.Packet;
import org.ungs.core.Registry;
import org.ungs.core.Scheduler;

@Getter
@RequiredArgsConstructor
public abstract class RoutingApplication {

  private final Registry registry;
  private final Scheduler scheduler;
  private final Node node;

  public abstract void onTick();

  public Node.Id getNodeId() {
    return node.getId();
  }

  public Optional<Packet> getNextPacket() {
    return Optional.ofNullable(node.getNextPacket());
  }
}
