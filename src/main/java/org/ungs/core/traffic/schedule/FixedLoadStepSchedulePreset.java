package org.ungs.core.traffic.schedule;

import org.ungs.core.config.InjectionScheduleConfig;

public final class FixedLoadStepSchedulePreset implements InjectionSchedulePreset {

  @Override
  public InjectionScheduleType type() {
    return InjectionScheduleType.FIXED_LOAD_STEP;
  }

  @Override
  public InjectionSchedule create(InjectionScheduleConfig cfg) {
    return null;
  }
}
