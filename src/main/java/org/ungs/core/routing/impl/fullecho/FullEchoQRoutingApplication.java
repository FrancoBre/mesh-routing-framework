package org.ungs.core.routing.impl.fullecho;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Node;
import org.ungs.core.observability.events.PacketDeliveredEvent;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.core.routing.api.RoutingApplication;

@Slf4j
public class FullEchoQRoutingApplication extends RoutingApplication {

  private static final double ETA = 0.5; // learning rate
  private static final double EPSILON_EQ_TOL = 1e-6; // tie tolerance
  private static final double STEP_TIME = 1.0;

  @Getter private final QTable qTable;

  public FullEchoQRoutingApplication(Node node) {
    super(node);
    this.qTable = new QTable();
  }

  @Override
  public AlgorithmType getType() {
    return AlgorithmType.FULL_ECHO_Q_ROUTING;
  }

  @Override
  public void onTick(SimulationRuntimeContext ctx) {
    var packetOpt = this.getNextPacket();
    if (packetOpt.isEmpty()) return;

    var packet = packetOpt.get();
    var destination = packet.getDestination();

    // delivered
    if (this.getNodeId().equals(destination)) {
      log.info(
          "[nodeId={}, time={}]: Packet {} has reached its destination",
          this.getNodeId(),
          ctx.getTick(),
          packet.getId());

      ctx.getEventSink().emit(new PacketDeliveredEvent(packet, 0, ctx.getTick(), this.getType()));
      return;
    }

    // deterministic neighbor order
    var neighbors = new ArrayList<>(this.getNode().getNeighbors());
    neighbors.sort(Comparator.comparing(n -> n.getId().value()));

    // === FULL ECHO STEP ===
    // query every neighbor for its best estimate to destination
    // and adjust Qx(d,y) for each before choosing.
    double q = packet.getTimeInQueue();

    for (Node y : neighbors) {
      double neighborEstimate = estimateFromNeighbor(y, destination); // min_z Q_y(d,z)

      double oldQ = qTable.get(this.getNodeId(), y.getId(), destination);

      // TD target: q + step + neighborEstimate
      double target = q + STEP_TIME + neighborEstimate;

      double newQ = oldQ + ETA * (target - oldQ);

      qTable.set(this.getNodeId(), y.getId(), destination, newQ);

      log.debug(
          "[full-echo] time={} node={} updated Q(from={}, to={}, dest={}) old={} new={} (neighborEstimate={})",
          ctx.getTick(),
          this.getNodeId(),
          this.getNodeId(),
          y.getId(),
          destination,
          String.format("%.4f", oldQ),
          String.format("%.4f", newQ),
          String.format("%.4f", neighborEstimate));
    }

    // === CHOOSE NEXT HOP USING UPDATED Qx(d,y) ===
    List<Double> qValues = new ArrayList<>(neighbors.size());
    for (Node y : neighbors) {
      qValues.add(qTable.get(this.getNodeId(), y.getId(), destination));
    }

    double minQ = qValues.stream().min(Double::compare).orElse(Double.MAX_VALUE);

    List<Node> bestCandidates = new ArrayList<>();
    for (int i = 0; i < neighbors.size(); i++) {
      if (Math.abs(qValues.get(i) - minQ) < EPSILON_EQ_TOL) {
        bestCandidates.add(neighbors.get(i));
      }
    }

    Node bestNextNode =
        (bestCandidates.size() > 1)
            ? bestCandidates.get(ctx.getRng().nextIndex(bestCandidates.size()))
            : bestCandidates.getFirst();

    log.debug(
        "[onTick/full-echo] time={} node={} chose next {} for packet {} (minQ={})",
        ctx.getTick(),
        this.getNodeId(),
        bestNextNode.getId(),
        packet.getId(),
        String.format("%.4f", minQ));

    ctx.schedule(this.getNodeId(), bestNextNode.getId(), packet);
  }

  private double estimateFromNeighbor(Node neighbor, Node.Id destination) {
    var app = (FullEchoQRoutingApplication) neighbor.getApplication();

    var neighborNeighbors = neighbor.getNeighbors();
    if (neighborNeighbors.isEmpty()) return 0.0;

    double min = Double.MAX_VALUE;
    for (Node z : neighborNeighbors) {
      double q = app.getQTable().get(neighbor.getId(), z.getId(), destination);
      if (q < min) min = q;
    }

    return (min == Double.MAX_VALUE) ? 0.0 : min;
  }

  private static class QTable {

    @Getter private final Set<QValue> qValues;

    public QTable() {
      this.qValues = new HashSet<>();
    }

    public double get(Node.Id from, Node.Id to, Node.Id destination) {
      return qValues.stream()
          .filter(
              q ->
                  q.getFrom().equals(from)
                      && q.getTo().equals(to)
                      && q.getDestination().equals(destination))
          .map(QValue::getValue)
          .findFirst()
          .orElse(0.0);
    }

    public void set(Node.Id from, Node.Id to, Node.Id destination, double value) {
      qValues.stream()
          .filter(
              q ->
                  q.getFrom().equals(from)
                      && q.getTo().equals(to)
                      && q.getDestination().equals(destination))
          .findFirst()
          .ifPresentOrElse(
              q -> q.setValue(value), () -> qValues.add(new QValue(from, to, destination, value)));
    }

    @Override
    public String toString() {
      if (qValues.isEmpty()) return "empty";
      StringBuilder sb = new StringBuilder();
      sb.append(String.format("\n%-8s %-8s %-12s %-8s%n", "FROM", "TO", "DESTINATION", "VALUE"));
      sb.append("------------------------------------------------\n");
      for (QValue q : qValues) {
        sb.append(
            String.format(
                "%-8s %-8s %-12s %-8.2f%n",
                q.from.value(), q.to.value(), q.destination.value(), q.value));
      }
      return sb.toString();
    }
  }

  @Getter
  @AllArgsConstructor
  private static class QValue {
    private final Node.Id from;
    private final Node.Id to;
    private final Node.Id destination;
    @Setter private double value;
  }
}
