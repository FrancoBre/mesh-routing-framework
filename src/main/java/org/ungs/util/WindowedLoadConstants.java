package org.ungs.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class WindowedLoadConstants {

  // Phase A
  public static final int PHASE_A_TICKS = 200; // duration
  public static final int PHASE_A_BATCH = 2; // packets per tick

  // Phase B
  public static final int PHASE_B_TICKS = 800; // duration
  public static final int PHASE_B_BATCH = 10; // packets per tick

  // After phases (0 = stop injecting)
  public static final int PHASE_C_BATCH = 0;
}
