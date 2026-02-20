package org.ungs.core.traffic.schedule;

import org.ungs.core.config.InjectionScheduleConfig;
import org.ungs.core.observability.events.LoadLevelUpdatedEvent;

public final class TriangularLoadSchedulePreset implements InjectionSchedulePreset {

  @Override
  public InjectionScheduleType type() {
    return InjectionScheduleType.TRIANGULAR_LOAD_LEVEL;
  }

  @Override
  public InjectionSchedule create(InjectionScheduleConfig cfg) {
    var c = (InjectionScheduleConfig.TriangularLoadLevel) cfg;

    final double minL = c.minL();
    final double maxL = c.maxL();
    final long period = c.periodTicks();

    if (period <= 0) {
      throw new IllegalArgumentException("triangular-load.period-ticks must be > 0");
    }
    if (maxL < minL) {
      throw new IllegalArgumentException("triangular-load.maxL must be >= minL");
    }

    return ctx -> {
      long t = (long) ctx.getTick();

      // Clamp to [0, period]
      if (t < 0) t = 0;
      if (t > period) t = period;

      // phase ∈ [0,1] up then down (triangle)
      // 0..0.5 rising, 0.5..1 falling
      double phase = (double) t / (double) period;

      double alpha;
      double L;
      LoadLevelUpdatedEvent.LoadLevelTrend trend;

      if (phase <= 0.5) {
        alpha = phase / 0.5; // 0..1
        L = minL + alpha * (maxL - minL);
        trend = LoadLevelUpdatedEvent.LoadLevelTrend.RISING;
      } else {
        alpha = (1.0 - phase) / 0.5; // 1..0
        L = minL + alpha * (maxL - minL);
        trend = LoadLevelUpdatedEvent.LoadLevelTrend.FALLING;
      }

      int base = (int) Math.floor(L);
      double frac = L - base;

      int inject = base;
      if (frac > 2.0) {
        if (ctx.getRng().nextUnitDouble() < frac) inject += 1;
      }

      ctx.getEventSink().emit(new LoadLevelUpdatedEvent(ctx.getTick(), L, trend));
      return inject;
    };
  }
}
