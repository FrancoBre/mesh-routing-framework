package org.ungs.core.traffic.schedule;

import java.util.EnumMap;
import java.util.Map;
import org.ungs.core.config.InjectionScheduleConfig;

public final class InjectionScheduleFactory {

  private static final Map<InjectionScheduleType, InjectionSchedulePreset> registry =
      new EnumMap<>(InjectionScheduleType.class);

  static {
    register(new LoadLevelSchedulePreset());
    register(new GapSchedulePreset());
    register(new ProbPerTickSchedulePreset());
    register(new WindowedLoadSchedulePreset());
    register(new PlateauThenLinearSchedulePreset());
    register(new PlateauRampPlateauSchedulePreset());
    register(new FixedLoadStepSchedulePreset());
  }

  private static void register(InjectionSchedulePreset preset) {
    registry.put(preset.type(), preset);
  }

  public static InjectionSchedule from(InjectionScheduleConfig cfg) {
    InjectionSchedulePreset preset = registry.get(cfg.type());
    if (preset == null)
      throw new IllegalArgumentException("Unknown traffic schedule: " + cfg.type());
    return preset.create(cfg);
  }

  private InjectionScheduleFactory() {}
}
