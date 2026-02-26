package org.ungs.core.routing.impl.qrouting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
public class QRoutingApplication extends RoutingApplication {

  private static final double ETA = 0.7; // learning rate
  private static final double EPSILON_EQ_TOL = 1e-6; // for comparing doubles (not exploration)
  private static final double STEP_TIME = 1.0;
  private static final double INITIAL_Q = 0.0;

  @Getter private final QTable qTable;

  public QRoutingApplication(Node node, SimulationRuntimeContext ctx) {
    super(node);
    this.qTable = new QTable();

    for (Node neighbor : node.getNeighbors()) {
      for (Node dest : ctx.getNetwork().getNodes()) {
        if (dest.getId().equals(node.getId())) continue;
        qTable.set(node.getId(), neighbor.getId(), dest.getId(), INITIAL_Q);
      }
    }
  }

  @Override
  public void onTickStart(SimulationRuntimeContext ctx) {
    qTable.takeSnapshot();
  }

  public AlgorithmType getType() {
    return AlgorithmType.Q_ROUTING;
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
          "[nodeId={}, time={}]: Packet {} has reached its destination (departed={}, transit={})",
          this.getNodeId(),
          ctx.getTick(),
          packetToProcess.getId(),
          packetToProcess.getDepartureTime(),
          ctx.getTick() - packetToProcess.getDepartureTime());

      ctx.getEventSink()
          .emit(new PacketDeliveredEvent(packetToProcess, ctx.getTick(), this.getType()));
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

    List<Double> qValues = new ArrayList<>();

    for (Node neighbor : neighbors) {
      double qValue =
          qTable.get(this.getNodeId(), neighbor.getId(), packetToProcess.getDestination());
      qValues.add(qValue);
    }

    double minQ = qValues.stream().min(Double::compare).orElse(0.0);

    List<Node> bestCandidates = new ArrayList<>();
    for (int i = 0; i < neighbors.size(); i++) {
      if (Math.abs(qValues.get(i) - minQ) < EPSILON_EQ_TOL) {
        bestCandidates.add(neighbors.get(i));
      }
    }

    // Random tie-break among best candidates
    Node bestNextNode;
    if (bestCandidates.size() > 1) {
      bestNextNode = bestCandidates.get(ctx.getRng().nextIndex(bestCandidates.size()));
    } else {
      bestNextNode = bestCandidates.getFirst();
    }

    log.debug("[onTick] Time={} - NodeId={} - QTable={}", ctx.getTick(), this.getNodeId(), qTable);
    log.debug(
        "[onTick] Time={} - NodeId={} - Chose next node {} for packet {}",
        ctx.getTick(),
        this.getNodeId(),
        bestNextNode.getId(),
        packetToProcess.getId());

    // temporal-difference update
    double oldEstimation =
        qTable.get(this.getNodeId(), bestNextNode.getId(), packetToProcess.getDestination());

    // next node's best estimate (min Q-value among its neighbors)
    Node nextNode = bestNextNode;
    var nextNodeApp = (QRoutingApplication) nextNode.getApplication();

    // If next node is isolated, skip Q-value update (invalid information)
    if (nextNode.getNeighbors().isEmpty()) {
      log.warn(
          "[nodeId={}, time={}]: Next node {} is isolated - skipping Q-update for packet {}",
          this.getNodeId(),
          ctx.getTick(),
          nextNode.getId(),
          packetToProcess.getId());
      ctx.schedule(this.getNodeId(), bestNextNode.getId(), packetToProcess);
      return;
    }

    double minNextQ = Double.MAX_VALUE;
    for (Node neighborOfNext : nextNode.getNeighbors()) {
      double qVal =
          nextNodeApp
              .getQTable()
              .getFromSnapshot(
                  nextNode.getId(), neighborOfNext.getId(), packetToProcess.getDestination());
      if (qVal < minNextQ) {
        minNextQ = qVal;
      }
    }

    double q = packetToProcess.getTimeInQueue();
    double t = minNextQ;

    double delta = ETA * ((q + STEP_TIME + t) - oldEstimation);
    double newValue = oldEstimation + delta;

    log.info(
        "[tick] Time={} - NodeId={} - Updating Q-value [from={}, destination={}, to={}] from {} to {}",
        ctx.getTick(),
        this.getNodeId(),
        this.getNodeId(),
        packetToProcess.getDestination(),
        bestNextNode.getId(),
        String.format("%.2f", oldEstimation),
        String.format("%.2f", newValue));

    qTable.set(this.getNodeId(), bestNextNode.getId(), packetToProcess.getDestination(), newValue);

    ctx.schedule(this.getNodeId(), bestNextNode.getId(), packetToProcess);
  }

  public static class QTable {

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
          .orElse(INITIAL_Q);
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

    // ── snapshot support (for tick-parallel simulation) ──────────────────

    private final Map<String, Double> snapshot = new HashMap<>();

    private static String snapshotKey(Node.Id from, Node.Id to, Node.Id dest) {
      return from.value() + ":" + to.value() + ":" + dest.value();
    }

    public void takeSnapshot() {
      snapshot.clear();
      for (QValue qv : qValues) {
        snapshot.put(snapshotKey(qv.getFrom(), qv.getTo(), qv.getDestination()), qv.getValue());
      }
    }

    /** Read from the start-of-tick snapshot (used by neighbor queries). */
    public double getFromSnapshot(Node.Id from, Node.Id to, Node.Id destination) {
      return snapshot.getOrDefault(snapshotKey(from, to, destination), INITIAL_Q);
    }

    @Override
    public String toString() {
      if (qValues.isEmpty()) {
        return "empty";
      }
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
  public static class QValue {

    private final Node.Id from;
    private final Node.Id to;
    private final Node.Id destination;

    @Setter private double value;
  }
}
