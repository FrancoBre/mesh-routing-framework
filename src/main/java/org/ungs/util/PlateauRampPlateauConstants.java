package org.ungs.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PlateauRampPlateauConstants {

  // --------- PHASE 1: plateau inicial ----------
  public static final int P1_TICKS = 200;
  public static final int P1_INJECT_EVERY_N_TICKS = 5;
  public static final int P1_BATCH_SIZE = 2;

  // --------- PHASE 2: rampa ----------
  // en rampa inyectás cada N ticks
  public static final int RAMP_INJECT_EVERY_N_TICKS = 1;

  // empieza en este batch size
  public static final int RAMP_START_BATCH_SIZE = 2;

  // cada cuántos ticks aumenta el batch en +1
  public static final int RAMP_INCREASE_EVERY_N_TICKS = 200;

  // duración máxima de la rampa (por tiempo)
  public static final int RAMP_TICKS = 600;

  // techo: no subir más que esto
  public static final int RAMP_MAX_BATCH_SIZE = 10;

  // --------- PHASE 3: plateau alto ----------
  public static final int P3_INJECT_EVERY_N_TICKS = 1;
  public static final int P3_BATCH_SIZE = 10; // típicamente = RAMP_MAX_BATCH_SIZE
}
