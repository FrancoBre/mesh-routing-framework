package org.ungs.core.observability.metrics.api;

import java.nio.file.Path;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.routing.api.AlgorithmType;

public interface MetricRenderer<T> {

  void renderPerAlgorithm(Path out, AlgorithmType algo, SimulationConfigContext cfg, T data);
}
