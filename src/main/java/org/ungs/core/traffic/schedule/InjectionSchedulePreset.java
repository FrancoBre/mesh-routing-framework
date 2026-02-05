package org.ungs.core.traffic.schedule;

import org.ungs.core.config.InjectionScheduleConfig;

public interface InjectionSchedulePreset {

  InjectionScheduleType type();

  InjectionSchedule create(InjectionScheduleConfig cfg);
}
