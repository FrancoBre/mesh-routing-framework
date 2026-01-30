package org.ungs.core.observability.metrics.impl.avgdelivery;

import java.util.ArrayList;
import java.util.List;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Packet;
import org.ungs.core.observability.api.SimulationEvent;
import org.ungs.core.observability.api.SimulationObserver;
import org.ungs.core.observability.events.PacketDeliveredEvent;
import org.ungs.core.observability.metrics.api.Metric;
import org.ungs.util.Tuple;

public final class AvgDeliveryTimeMetric
    implements Metric<List<Tuple<Double, Double>>>, SimulationObserver {

  private final long warmupTicks;
  private final int sampleEvery;

  private final List<Packet> deliveredPackets;
  private final List<Tuple<Double, Double>> series = new ArrayList<>();

  public AvgDeliveryTimeMetric(long warmupTicks, int sampleEvery) {
    this.warmupTicks = Math.max(0, warmupTicks);
    this.deliveredPackets = new ArrayList<>();
    if (sampleEvery <= 0) throw new IllegalArgumentException("sampleEvery must be > 0");
    this.sampleEvery = sampleEvery;
  }

  @Override
  public void reset() {
    series.clear();
  }

  @Override
  public void onEvent(SimulationEvent e, SimulationRuntimeContext ctx) {
    if (e instanceof PacketDeliveredEvent h) {
      h.packet().markAsArrived(ctx);
      deliveredPackets.add(h.packet());

      double t = ctx.getTick();
      if (t < warmupTicks) return;
      if (t % sampleEvery != 0) return;

      double avg =
          deliveredPackets.stream()
              .mapToDouble(
                  (Packet p) ->
                      p.getArrivalTime() - p.getDepartureTime()) // FIXME: esto creo que está mal
              .average()
              .orElse(0.0);

      series.add(new Tuple<>(t, avg));
    }
  }

  @Override
  public List<Tuple<Double, Double>> snapshot() {
    return List.copyOf(series);
  }

  @Override
  public void onAlgorithmEnd(SimulationRuntimeContext ctx) {
    deliveredPackets.clear();
  }
}
