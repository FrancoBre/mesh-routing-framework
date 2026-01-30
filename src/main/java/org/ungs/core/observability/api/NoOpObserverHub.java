package org.ungs.core.observability.api;

import lombok.NoArgsConstructor;
import org.ungs.core.engine.SimulationRuntimeContext;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class NoOpObserverHub implements ObserverHub {

  public static final NoOpObserverHub INSTANCE = new NoOpObserverHub();

  @Override
  public void onSimulationStart(SimulationRuntimeContext ctx) {}

  @Override
  public void onSimulationEnd(SimulationRuntimeContext ctx) {}

  @Override
  public void emit(SimulationEvent event) {}
}
