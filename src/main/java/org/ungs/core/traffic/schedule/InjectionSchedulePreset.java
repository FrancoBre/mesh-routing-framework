package org.ungs.core.traffic.schedule;

import org.ungs.core.config.InjectionScheduleConfig;

public sealed interface InjectionSchedulePreset
    permits LoadLevelSchedulePreset,
        GapSchedulePreset,
        ProbPerTickSchedulePreset,
        WindowedLoadSchedulePreset,
        PlateauThenLinearSchedulePreset,
        PlateauRampPlateauSchedulePreset,
        FixedLoadStepSchedulePreset {

  InjectionScheduleType type();

  InjectionSchedule create(InjectionScheduleConfig cfg);
}
