package org.ungs.core.observability.output.api;

import org.ungs.core.observability.api.SimulationObserver;

public final class OutputBundle {

  private final String id;
  private final SimulationObserver observer;

  public OutputBundle(String id, SimulationObserver observer) {
    this.id = id;
    this.observer = observer;
  }

  public String id() {
    return id;
  }

  public SimulationObserver observer() {
    return observer;
  }
}
