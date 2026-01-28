package org.ungs.core.observability.api;

public interface EventSink {

  void emit(SimulationEvent event);
}
