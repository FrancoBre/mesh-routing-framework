package org.ungs.core.traffic.schedule;

public enum InjectionScheduleType {
  LOAD_LEVEL,
  PROB_PER_TICK,
  GAP,
  WINDOWED_LOAD,
  PLATEAU_THEN_LINEAR,
  PLATEAU_RAMP_PLATEAU,
  FIXED_LOAD_STEP
}
