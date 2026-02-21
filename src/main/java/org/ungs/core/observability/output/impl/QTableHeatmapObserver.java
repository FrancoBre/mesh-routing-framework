package org.ungs.core.observability.output.impl;

import static org.ungs.core.routing.api.AlgorithmType.FULL_ECHO_Q_ROUTING;
import static org.ungs.core.routing.api.AlgorithmType.Q_ROUTING;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Network;
import org.ungs.core.network.Node;
import org.ungs.core.observability.api.QTableSnapshotEvent;
import org.ungs.core.observability.api.SimulationEvent;
import org.ungs.core.observability.api.SimulationObserver;
import org.ungs.core.observability.events.TickEvent;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.core.routing.api.RoutingApplication;
import org.ungs.core.routing.impl.fullecho.FullEchoQRoutingApplication;
import org.ungs.core.routing.impl.qrouting.QRoutingApplication;

@Slf4j
public final class QTableHeatmapObserver implements SimulationObserver {

  private static final Node.Id FROM = new Node.Id(14);
  private static final Node.Id DESTINATION = new Node.Id(35);

  List<Node.Id> destinations = List.of(DESTINATION);

  private final Network network;
  private final Path outDir;
  private final List<QTableSnapshotEvent> snapshots = new ArrayList<>();

  private Node.Id lastBestTo = null;

  private final QTableHeatmapRenderer renderer = new QTableHeatmapRenderer();

  public QTableHeatmapObserver(Network network, Path outDir) {
    this.network = network;
    this.outDir = outDir;
  }

  @Override
  public void onEvent(SimulationEvent e, SimulationRuntimeContext ctx) {
    if (!Q_ROUTING.equals(ctx.getCurrentAlgorithm())
        && !FULL_ECHO_Q_ROUTING.equals(ctx.getCurrentAlgorithm())) {
      return;
    }

    if (e instanceof QTableSnapshotEvent q) {
      snapshots.add(q);
      return;
    }

    if (e instanceof TickEvent t) {
      int tick = (int) ctx.getTick();

      if (tick % 500 != 0) {
        return;
      }

      AlgorithmType algo = t.algorithm();

      List<QTableSnapshotEvent> frame =
          snapshots.stream().filter(s -> s.algorithm() == algo).toList();

      Path outFile =
          outDir
              .resolve(algo.name())
              .resolve("outputs")
              .resolve("q-heatmap")
              .resolve(String.format("tick-%05d.png", tick));

      QTableHeatmapRenderer.QGetter qGetter =
          (fromId, toId, destId) -> {
            RoutingApplication app;
            if (Q_ROUTING.equals(algo)) {
              app = network.getNode(fromId).getApplication();
              return ((QRoutingApplication) app).getQTable().get(fromId, toId, destId);
            } else if (FULL_ECHO_Q_ROUTING.equals(algo)) {
              app = network.getNode(fromId).getApplication();
              return ((FullEchoQRoutingApplication) app).getQTable().get(fromId, toId, destId);
            } else {
              throw new IllegalStateException("Unexpected algorithm: " + algo);
            }
          };

      if (tick % 50 == 0) {
        logBestVsSecond(ctx, algo, tick);
      }

      List<Node.Id> actionNeighbors =
          network.getNode(FROM).getNeighbors().stream().map(Node::getId).toList();

      renderer.render(
          (long) ctx.getTick(),
          ctx.getCurrentAlgorithm().name(),
          FROM,
          actionNeighbors,
          destinations,
          DESTINATION,
          qGetter,
          outFile);

      snapshots.clear();
    }
  }

  private void logBestVsSecond(SimulationRuntimeContext ctx, AlgorithmType algo, int tick) {
    var fromNode = network.getNode(FROM);
    List<Node.Id> actionNeighbors = fromNode.getNeighbors().stream().map(Node::getId).toList();

    var app = fromNode.getApplication();

    Object qTable;
    List<Row> rows = new ArrayList<>();
    if (app instanceof QRoutingApplication q) {
      qTable = q.getQTable();
      // Rankear neighbors por Q(from,to,DEST)
      for (var to : actionNeighbors) {
        double v = ((QRoutingApplication.QTable) qTable).get(FROM, to, DESTINATION);
        rows.add(new Row(to, v));
      }
    } else if (app instanceof FullEchoQRoutingApplication fe) {
      qTable = fe.getQTable();
      for (var to : actionNeighbors) {
        double v = ((FullEchoQRoutingApplication.QTable) qTable).get(FROM, to, DESTINATION);
        rows.add(new Row(to, v));
      }
    } else {
      return;
    }

    rows.sort(Comparator.comparingDouble(r -> r.q));

    if (rows.isEmpty()) return;

    Row best = rows.get(0);
    Row second = rows.size() > 1 ? rows.get(1) : null;

    boolean bestChanged = lastBestTo != null && !lastBestTo.equals(best.to);
    if (lastBestTo == null) bestChanged = false;

    double gap = (second == null) ? Double.NaN : (second.q - best.q);

    log.info(
        "[osc] algo={} tick={} from={} dest={} bestTo={} bestQ={} secondTo={} secondQ={} gap={} bestChanged={}",
        algo.name(),
        tick,
        FROM.value(),
        DESTINATION.value(),
        best.to.value(),
        best.q,
        second == null ? "-" : second.to.value(),
        second == null ? Double.NaN : second.q,
        gap,
        bestChanged);

    //      try (BufferedWriter writer = new BufferedWriter(new FileWriter("osc.txt", true))) {
    //          writer.write(String.format(
    //              "[osc] algo=%s tick=%d from=%d dest=%d bestTo=%d bestQ=%f secondTo=%s secondQ=%f
    // gap=%f bestChanged=%b%n",
    //              algo.name(),
    //              tick,
    //              FROM.value(),
    //              DESTINATION.value(),
    //              best.to.value(),
    //              best.q,
    //              second == null ? "-" : second.to.value(),
    //              second == null ? Double.NaN : second.q,
    //              gap,
    //              bestChanged
    //          ));
    //      } catch (IOException e) {
    //          e.printStackTrace();
    //      }

    lastBestTo = best.to;
  }

  private static final class Row {
    final Node.Id to;
    final double q;

    Row(Node.Id to, double q) {
      this.to = to;
      this.q = q;
    }
  }
}
