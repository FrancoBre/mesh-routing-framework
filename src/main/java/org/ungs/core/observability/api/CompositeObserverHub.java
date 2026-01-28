package org.ungs.core.observability.api;

import java.util.List;
import org.ungs.core.engine.SimulationRuntimeContext;

public final class CompositeObserverHub implements ObserverHub, EventSink {

  private final List<SimulationObserver> observers;

  private SimulationRuntimeContext currentCtx;

  public CompositeObserverHub(List<SimulationObserver> observers) {
    this.observers = List.copyOf(observers);
  }

  public void bindContext(SimulationRuntimeContext ctx) {
    this.currentCtx = ctx;
  }

  @Override
  public void emit(SimulationEvent event) {
    for (SimulationObserver o : observers) {
      o.onEvent(event, currentCtx);
    }
  }

  @Override
  public void onAlgorithmStart(SimulationRuntimeContext ctx) {
    bindContext(ctx);
    observers.forEach(o -> o.onAlgorithmStart(ctx));
  }

  @Override
  public void onAlgorithmEnd(SimulationRuntimeContext ctx) {
    observers.forEach(o -> o.onAlgorithmEnd(ctx));
  }

  @Override
  public void onSimulationEnd(SimulationRuntimeContext ctx) {
    observers.forEach(o -> o.onSimulationEnd(ctx));
  }
}
