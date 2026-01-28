package org.ungs.core.traffic.schedule;

import org.ungs.core.config.InjectionScheduleConfig;

public final class ProbPerTickSchedulePreset implements InjectionSchedulePreset {

  @Override
  public InjectionScheduleType type() {
    return InjectionScheduleType.PROB_PER_TICK;
  }

  @Override
  public InjectionSchedule create(InjectionScheduleConfig cfg) {
    var c = (InjectionScheduleConfig.ProbPerTick) cfg;
    double p = c.p();

    return ctx -> ctx.getRng().nextUnitDouble() < p ? 1 : 0;
  }
}
