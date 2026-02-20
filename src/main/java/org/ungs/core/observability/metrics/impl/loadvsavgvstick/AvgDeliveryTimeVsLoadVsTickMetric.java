package org.ungs.core.observability.metrics.impl.loadvsavgvstick;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.observability.api.SimulationEvent;
import org.ungs.core.observability.api.SimulationObserver;
import org.ungs.core.observability.events.LoadLevelUpdatedEvent;
import org.ungs.core.observability.events.PacketDeliveredEvent;
import org.ungs.core.observability.metrics.api.Metric;
import org.ungs.util.Tuple3;

public final class AvgDeliveryTimeVsLoadVsTickMetric
    implements Metric<List<Tuple3<Long, Double, Double>>>, SimulationObserver {

  private final long warmupTicks;
  private final int sampleEvery;
  private final int windowSize;

  private final ArrayDeque<Double> lastDelays = new ArrayDeque<>();
  private double lastLoadLevel = Double.NaN;

  // tick, avgDelayMovingAvg, loadLevel
  private final List<Tuple3<Long, Double, Double>> series = new ArrayList<>();

  public AvgDeliveryTimeVsLoadVsTickMetric(long warmupTicks, int sampleEvery, int windowSize) {
    this.warmupTicks = Math.max(0, warmupTicks);
    if (sampleEvery <= 0) throw new IllegalArgumentException("sampleEvery must be > 0");
    if (windowSize <= 0) throw new IllegalArgumentException("windowSize must be > 0");
    this.sampleEvery = sampleEvery;
    this.windowSize = windowSize;
  }

  @Override
  public void reset() {
    series.clear();
    lastDelays.clear();
    lastLoadLevel = Double.NaN;
  }

  @Override
  public void onEvent(SimulationEvent e, SimulationRuntimeContext ctx) {

    if (e instanceof LoadLevelUpdatedEvent lle) {
      lastLoadLevel = lle.loadLevel();
      return;
    }

    if (e instanceof PacketDeliveredEvent pd) {
      pd.packet().markAsArrived(ctx);

      long t = (long) ctx.getTick();
      if (t < warmupTicks) return;

      double delay = pd.packet().getArrivalTime() - pd.packet().getDepartureTime();
      lastDelays.addLast(delay);
      while (lastDelays.size() > windowSize) lastDelays.removeFirst();

      if (t % sampleEvery != 0) return;
      if (Double.isNaN(lastLoadLevel)) return;
      if (lastDelays.isEmpty()) return;

      double avg = lastDelays.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
      series.add(new Tuple3<>(t, avg, lastLoadLevel));
    }
  }

  @Override
  public List<Tuple3<Long, Double, Double>> snapshot() {
    return List.copyOf(series);
  }
}
