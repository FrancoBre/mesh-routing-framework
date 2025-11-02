package org.ungs.core;

import java.util.List;
import org.ungs.cli.SimulationConfigLoader;
import org.ungs.export.ExportType;
import org.ungs.metrics.MetricType;
import org.ungs.routing.AlgorithmType;

public record SimulationConfig(
    TopologyType topology,
    List<AlgorithmType> algorithms,
    int totalPackets,
    int packetInjectGap,
    long seed,
    List<MetricType> metrics,
    List<ExportType> exportTo) {

  public static SimulationConfig fromConfigLoader(SimulationConfigLoader configLoader) {
    if (configLoader.totalPackets() <= 0) {
      throw new IllegalArgumentException("totalPackets must be positive");
    }

    if (configLoader.packetInjectGap() < 0) {
      throw new IllegalArgumentException("packetInjectGap cannot be negative");
    }

    return new SimulationConfig(
        configLoader.topology(),
        configLoader.algorithm().stream()
            .map(String::toUpperCase)
            .map(AlgorithmType::valueOf)
            .toList(),
        configLoader.totalPackets(),
        configLoader.packetInjectGap(),
        configLoader.seed(),
        configLoader.metrics().stream().map(String::toUpperCase).map(MetricType::valueOf).toList(),
        configLoader.exportTo().stream()
            .map(String::toUpperCase)
            .map(ExportType::valueOf)
            .toList());
  }
}
