package org.ungs.core.observability.route;

import java.util.ArrayList;
import java.util.List;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.observability.api.SimulationEvent;
import org.ungs.core.observability.events.HopEvent;
import org.ungs.core.observability.metrics.api.Metric;

public final class RouteMetric implements Metric<List<HopEvent>> {

  private final List<HopEvent> hops = new ArrayList<>();

  @Override
  public void reset() {
    hops.clear();
  }

  @Override
  public void onEvent(SimulationEvent e, SimulationRuntimeContext ctx) {
    if (e instanceof HopEvent h) hops.add(h);
  }

  @Override
  public List<HopEvent> snapshot() {
    return List.copyOf(hops);
  }
}
