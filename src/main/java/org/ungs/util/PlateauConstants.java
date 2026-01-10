package org.ungs.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PlateauConstants {

  public static final int PLATEAU_TICKS = 500; // k tiempo
  public static final int PLATEAU_INJECT_EVERY_N_TICKS = 5; // "inyecto cada n gaps"
  public static final int PLATEAU_BATCH_SIZE = 2; // 1/2/3 paquetes por evento (carga constante)

  public static final int RAMP_INJECT_EVERY_N_TICKS = 1; // durante la rampa, cada cuánto inyectás
  public static final int RAMP_START_BATCH_SIZE = 2; // arrancá la rampa en 2 por tick (o 1)
  public static final int RAMP_INCREASE_EVERY_N_TICKS =
      200; // cada X ticks subís el batchSize en +1
}
