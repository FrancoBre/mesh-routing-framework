package org.ungs.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ungs.routing.RoutingApplication;

@Getter
@RequiredArgsConstructor
public class Node {

  private final Node.Id id;
  private final List<Node> neighbors;
  private final Deque<Packet> queue = new ArrayDeque<>();
  private final Network network;

  private RoutingApplication application;

  public void installApplication(RoutingApplication application) {
    this.application = application;
  }

  public void receivePacket(Packet packet) {
    queue.addLast(packet);
  }

  public Packet getNextPacket() {
    return queue.pollFirst();
  }

  public record Id(int value) {}

  @Override
  public String toString() {
    return "Node{" + "id=" + id + '}';
  }
}
