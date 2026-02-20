package org.ungs.core.traffic.schedule;

import org.ungs.core.config.InjectionScheduleConfig;
import org.ungs.core.observability.events.LoadLevelUpdatedEvent;

public final class LoadLevelSchedulePreset implements InjectionSchedulePreset {

  @Override
  public InjectionScheduleType type() {
    return InjectionScheduleType.LOAD_LEVEL;
  }

  @Override
  public InjectionSchedule create(InjectionScheduleConfig cfg) {
    var c = (InjectionScheduleConfig.LoadLevel) cfg;
    double L = c.L();

    return ctx -> {
      int base = (int) Math.floor(L);
      double frac = L - base;

      int inject = base;
      if (frac > 0.0) {
        if (ctx.getRng().nextUnitDouble() < frac) inject += 1;
      }

      ctx.getEventSink()
          .emit(
              new LoadLevelUpdatedEvent(
                  ctx.getTick(), L, LoadLevelUpdatedEvent.LoadLevelTrend.PLATEAU));
      return inject;
    };
  }
}
