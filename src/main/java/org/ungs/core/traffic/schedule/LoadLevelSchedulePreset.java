package org.ungs.core.traffic.schedule;

import org.ungs.core.config.InjectionScheduleConfig;

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
      return inject;
    };
  }
}
