package org.ungs.core.traffic.schedule;

import org.ungs.core.config.InjectionScheduleConfig;

public final class PlateauThenLinearSchedulePreset implements InjectionSchedulePreset {

  @Override
  public InjectionScheduleType type() {
    return InjectionScheduleType.PLATEAU_THEN_LINEAR;
  }

  @Override
  public InjectionSchedule create(InjectionScheduleConfig cfg) {
    return null;
  }
}
