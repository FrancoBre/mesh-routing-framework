package org.ungs.core.observability.metrics.impl.avgdelivery;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.observability.api.SimulationEvent;
import org.ungs.core.observability.api.SimulationObserver;
import org.ungs.core.observability.events.PacketDeliveredEvent;
import org.ungs.core.observability.metrics.api.Metric;
import org.ungs.util.Tuple;

public final class AvgDeliveryTimeMetric
    implements Metric<List<Tuple<Double, Double>>>, SimulationObserver {

  private static final int DEFAULT_WINDOW_SIZE = 500;

  private final long warmupTicks;
  private final int sampleEvery;
  private final int windowSize; // 0 = disabled (cumulative average)

  // Sliding window of recent delivery delays
  private final ArrayDeque<Double> lastDelays = new ArrayDeque<>();
  private final List<Tuple<Double, Double>> series = new ArrayList<>();

  public AvgDeliveryTimeMetric(long warmupTicks, int sampleEvery) {
    this(warmupTicks, sampleEvery, DEFAULT_WINDOW_SIZE);
  }

  public AvgDeliveryTimeMetric(long warmupTicks, int sampleEvery, int windowSize) {
    this.warmupTicks = Math.max(0, warmupTicks);
    if (sampleEvery <= 0) throw new IllegalArgumentException("sampleEvery must be > 0");
    if (windowSize < 0)
      throw new IllegalArgumentException("windowSize must be >= 0 (0 = disabled)");
    this.sampleEvery = sampleEvery;
    this.windowSize = windowSize;
  }

  @Override
  public void reset() {
    series.clear();
    lastDelays.clear();
  }

  @Override
  public void onEvent(SimulationEvent e, SimulationRuntimeContext ctx) {
    if (e instanceof PacketDeliveredEvent h) {
      h.packet().markAsArrived(ctx);

      double t = ctx.getTick();
      if (t < warmupTicks) return;

      // Add delay to collection
      double delay = h.packet().getArrivalTime() - h.packet().getDepartureTime();
      lastDelays.addLast(delay);

      // Apply sliding window limit only if windowSize > 0
      if (windowSize > 0) {
        while (lastDelays.size() > windowSize) {
          lastDelays.removeFirst();
        }
      }

      if (t % sampleEvery != 0) return;
      if (lastDelays.isEmpty()) return;

      // Average over window (or all packets if windowSize=0)
      double avg = lastDelays.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

      series.add(new Tuple<>(t, avg));
    }
  }

  @Override
  public List<Tuple<Double, Double>> snapshot() {
    return List.copyOf(series);
  }

  @Override
  public void onAlgorithmEnd(SimulationRuntimeContext ctx) {
    lastDelays.clear();
  }
}
