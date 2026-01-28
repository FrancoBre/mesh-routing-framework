package org.ungs.core.observability.route;

import java.nio.file.Path;
import java.util.List;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.observability.events.HopEvent;
import org.ungs.core.observability.metrics.api.MetricRenderer;
import org.ungs.core.routing.api.AlgorithmType;

public final class RouteHeatmapRenderer implements MetricRenderer<List<HopEvent>> {

  @Override
  public void renderPerAlgorithm(
      Path out, AlgorithmType algo, SimulationConfigContext cfg, List<HopEvent> hops) {
    // usa cfg.topology / ctx.network en el observability (o pasás Network acá si preferís)
  }
}
