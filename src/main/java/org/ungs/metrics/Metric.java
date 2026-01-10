package org.ungs.metrics;

import org.ungs.core.SimulationConfig;
import org.ungs.routing.AlgorithmType;

public interface Metric<T> {

  void collect();

  T report();

  void reset();

  void plot(String filename, AlgorithmType algorithmType, SimulationConfig config);
}
