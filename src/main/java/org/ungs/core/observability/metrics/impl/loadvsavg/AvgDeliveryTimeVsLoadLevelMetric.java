package org.ungs.core.observability.metrics.impl.loadvsavg;

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

public final class AvgDeliveryTimeVsLoadLevelMetric
    implements Metric<List<Tuple3<Double, Double, LoadLevelUpdatedEvent.LoadLevelTrend>>>,
        SimulationObserver {

  private final long warmupTicks;
  private final int sampleEvery;

  private final ArrayDeque<Double> lastDelays = new ArrayDeque<>();
  private final int windowSize = 500;

  private final List<Tuple3<Double, Double, LoadLevelUpdatedEvent.LoadLevelTrend>> series =
      new ArrayList<>();

  private double lastLoadLevel = Double.NaN;
  private LoadLevelUpdatedEvent.LoadLevelTrend lastTrend = null;

  public AvgDeliveryTimeVsLoadLevelMetric(long warmupTicks, int sampleEvery) {
    this.warmupTicks = Math.max(0, warmupTicks);
    if (sampleEvery <= 0) throw new IllegalArgumentException("sampleEvery must be > 0");
    this.sampleEvery = sampleEvery;
  }

  @Override
  public void reset() {
    series.clear();
    lastDelays.clear();
    lastLoadLevel = Double.NaN;
    lastTrend = null;
  }

  @Override
  public void onEvent(SimulationEvent e, SimulationRuntimeContext ctx) {

    if (e instanceof LoadLevelUpdatedEvent lle) {
      lastLoadLevel = lle.loadLevel();
      lastTrend = lle.trend();
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
      if (lastTrend == null) return;
      if (lastDelays.isEmpty()) return;

      double avg = lastDelays.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

      series.add(new Tuple3<>(lastLoadLevel, avg, lastTrend));
    }
  }

  @Override
  public List<Tuple3<Double, Double, LoadLevelUpdatedEvent.LoadLevelTrend>> snapshot() {
    return List.copyOf(series);
  }

  @Override
  public void onAlgorithmEnd(SimulationRuntimeContext ctx) {
    lastDelays.clear();
    lastLoadLevel = Double.NaN;
    lastTrend = null;
  }
}
