package org.ungs.core.observability.metrics.api;

import java.nio.file.Path;
import java.util.Map;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.routing.api.AlgorithmType;

public interface ComparisonRenderer<T> {

  void renderComparison(Path out, SimulationConfigContext cfg, Map<AlgorithmType, T> dataByAlgo);
}
