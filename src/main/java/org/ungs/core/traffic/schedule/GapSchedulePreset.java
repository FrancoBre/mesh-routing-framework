package org.ungs.core.traffic.schedule;

import org.ungs.core.config.InjectionScheduleConfig;

public final class GapSchedulePreset implements InjectionSchedulePreset {

  @Override
  public InjectionScheduleType type() {
    return InjectionScheduleType.GAP;
  }

  @Override
  public InjectionSchedule create(InjectionScheduleConfig cfg) {
    var c = (InjectionScheduleConfig.Gap) cfg;

    return ctx -> (ctx.getTick() % c.injectEveryNTicks() == 0) ? c.batchSize() : 0;
  }
}
