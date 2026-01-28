package org.ungs.core.traffic.schedule;

import org.ungs.core.config.InjectionScheduleConfig;

public final class PlateauRampPlateauSchedulePreset implements InjectionSchedulePreset {

  @Override
  public InjectionScheduleType type() {
    return InjectionScheduleType.PLATEAU_RAMP_PLATEAU;
  }

  @Override
  public InjectionSchedule create(InjectionScheduleConfig cfg) {
    return null;
  }
}
