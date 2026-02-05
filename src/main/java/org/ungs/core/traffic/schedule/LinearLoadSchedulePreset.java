package org.ungs.core.traffic.schedule;

import org.ungs.core.config.InjectionScheduleConfig;
import org.ungs.core.observability.events.LoadLevelUpdatedEvent;

public final class LinearLoadSchedulePreset implements InjectionSchedulePreset {

  @Override
  public InjectionScheduleType type() {
    return InjectionScheduleType.LINEAR_LOAD_LEVEL;
  }

  @Override
  public InjectionSchedule create(InjectionScheduleConfig cfg) {
    var c = (InjectionScheduleConfig.LinearLoadLevel) cfg;

    final double minL = c.minL();
    final double maxL = c.maxL();
    final long period = c.periodTicks();

    if (period <= 0) {
      throw new IllegalArgumentException("linear-load.period-ticks must be > 0");
    }
    if (maxL < minL) {
      throw new IllegalArgumentException("linear-load.maxL must be >= minL");
    }

    return ctx -> {
      long t = (long) ctx.getTick();

      // Clamp to [0, period]
      if (t < 0) t = 0;
      if (t > period) t = period;

      // phase ∈ [0,1] monotonic
      double phase = (double) t / (double) period;

      // L increases from minL to maxL, then stays at maxL because of clamp above
      double L = minL + phase * (maxL - minL);

      // L -> injection count (floor + Bernoulli(frac))
      int base = (int) Math.floor(L);
      double frac = L - base;

      int inject = base;
      if (frac > 0.0) {
        if (ctx.getRng().nextUnitDouble() < frac) inject += 1;
      }

      ctx.getEventSink()
          .emit(
              new LoadLevelUpdatedEvent(
                  ctx.getTick(), L, LoadLevelUpdatedEvent.LoadLevelTrend.RISING));
      return inject;
    };
  }
}
