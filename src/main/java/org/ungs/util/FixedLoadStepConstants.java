package org.ungs.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FixedLoadStepConstants {

  // Cada cuánto cambia el nivel de carga
  public static final int STEP_TICKS = 200;

  // Inyectás cada N ticks (1 = “cada tick”)
  public static final int INJECT_EVERY_N_TICKS = 1;

  // Opción A: niveles explícitos (recomendado)
  public static final int[] BATCH_SIZES = new int[] {1, 2, 3, 4, 5, 6, 7};

  // Opción B: si preferís incremental simple (comentá BATCH_SIZES y usá esto)
  // public static final int START_BATCH = 1;
  // public static final int MAX_BATCH = 10;
}
