package org.ungs.core.observability.metrics.impl.windoweddelivery;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Packet;
import org.ungs.core.observability.api.SimulationEvent;
import org.ungs.core.observability.api.SimulationObserver;
import org.ungs.core.observability.events.PacketDeliveredEvent;
import org.ungs.core.observability.metrics.api.Metric;
import org.ungs.util.Tuple;

/**
 * Windowed average delivery time metric.
 *
 * <p>Instead of computing a cumulative average over ALL packets (which drowns out recent
 * performance changes), this metric computes the mean delivery time over only the last {@code
 * windowSize} delivered packets. This captures the "current" performance of the algorithm and makes
 * differences between algorithms visible even when they are small.
 */
public final class WindowedDeliveryTimeMetric
    implements Metric<List<Tuple<Double, Double>>>, SimulationObserver {

  private final long warmupTicks;
  private final int sampleEvery;
  private final int windowSize;

  /** Sliding window of recent transit times. */
  private final Deque<Double> window = new ArrayDeque<>();

  private double windowSum = 0.0;

  /** Time-series: (tick, windowedAvg). */
  private final List<Tuple<Double, Double>> series = new ArrayList<>();

  public WindowedDeliveryTimeMetric(long warmupTicks, int sampleEvery, int windowSize) {
    this.warmupTicks = Math.max(0, warmupTicks);
    if (sampleEvery <= 0) throw new IllegalArgumentException("sampleEvery must be > 0");
    this.sampleEvery = sampleEvery;
    if (windowSize <= 0) throw new IllegalArgumentException("windowSize must be > 0");
    this.windowSize = windowSize;
  }

  @Override
  public void reset() {
    series.clear();
    window.clear();
    windowSum = 0.0;
  }

  @Override
  public void onEvent(SimulationEvent e, SimulationRuntimeContext ctx) {
    if (e instanceof PacketDeliveredEvent h) {
      h.packet().markAsArrived(ctx);

      double t = ctx.getTick();
      if (t < warmupTicks) return;

      // Compute transit time for this packet
      Packet p = h.packet();
      double transit = p.getArrivalTime() - p.getDepartureTime();

      // Add to sliding window
      window.addLast(transit);
      windowSum += transit;

      // Evict oldest if window is full
      while (window.size() > windowSize) {
        windowSum -= window.removeFirst();
      }

      // Sample at regular intervals
      if (t % sampleEvery != 0) return;
      if (window.isEmpty()) return;

      double avg = windowSum / window.size();
      series.add(new Tuple<>(t, avg));
    }
  }

  @Override
  public List<Tuple<Double, Double>> snapshot() {
    return List.copyOf(series);
  }

  @Override
  public void onAlgorithmEnd(SimulationRuntimeContext ctx) {
    window.clear();
    windowSum = 0.0;
  }
}
