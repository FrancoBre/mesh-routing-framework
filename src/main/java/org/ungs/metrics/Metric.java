package org.ungs.metrics;

public interface Metric<T> {

  void collect();

  T report();

  void reset();

  void plot(String filename);
}
