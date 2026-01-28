package org.ungs.core.traffic.schedule;

import org.ungs.core.config.InjectionScheduleConfig;

public final class WindowedLoadSchedulePreset implements InjectionSchedulePreset {

  @Override
  public InjectionScheduleType type() {
    return InjectionScheduleType.WINDOWED_LOAD;
  }

  @Override
  public InjectionSchedule create(InjectionScheduleConfig cfg) {
    return null;
  }
}
