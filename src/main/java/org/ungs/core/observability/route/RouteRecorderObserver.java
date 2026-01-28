package org.ungs.core.observability.route;

import java.util.ArrayList;
import java.util.List;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.observability.api.SimulationEvent;
import org.ungs.core.observability.api.SimulationObserver;
import org.ungs.core.observability.events.HopEvent;

public final class RouteRecorderObserver implements SimulationObserver {

  private final List<HopEvent> route = new ArrayList<>();

  public List<HopEvent> snapshot() {
    return List.copyOf(route);
  }

  @Override
  public void onAlgorithmStart(SimulationRuntimeContext ctx) {
    route.clear();
  }

  @Override
  public void onEvent(SimulationEvent event, SimulationRuntimeContext ctx) {
    if (event instanceof HopEvent) {
      route.add((HopEvent) event);
    }
  }
}
