package org.ungs.core.engine;

import java.util.ArrayList;
import java.util.List;
import org.ungs.core.network.Node;
import org.ungs.core.network.Packet;

public class Scheduler {

  private static final Scheduler INSTANCE = new Scheduler();

  private final List<PendingSend> pendingSends;

  private Scheduler() {
    this.pendingSends = new ArrayList<>();
  }

  public static Scheduler getInstance() {
    return INSTANCE;
  }

  public List<PendingSend> flushPendingSends() {
    List<PendingSend> toSend = List.copyOf(pendingSends);
    pendingSends.clear();
    return toSend;
  }

  public void schedule(Node.Id from, Node.Id to, Packet packet) {
    pendingSends.add(new PendingSend(from, to, packet));
  }

  public record PendingSend(Node.Id from, Node.Id to, Packet packet) {}
}
