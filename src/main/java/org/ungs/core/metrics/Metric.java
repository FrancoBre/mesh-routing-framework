package org.ungs.core.metrics;

import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.routing.AlgorithmType;

public interface Metric<T> {

  void collect();

  T report();

  void reset();

  void plot(String filename, AlgorithmType algorithmType, SimulationConfigContext config);

  void plotAll(String filename, SimulationConfigContext config);
}
