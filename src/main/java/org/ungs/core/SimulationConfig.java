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
    int totalTicks,
    int packetInjectGap,
    long seed,
    List<MetricType> metrics,
    List<ExportType> exportTo,
    boolean linearIncrementalPacketInjection,
    boolean plateauThenLinearPacketInjection,
    boolean plateauRampPlateauPacketInjection,
    boolean fixedLoadStepPacketInjection,
    boolean windowedLoadPacketInjection,
    double loadLevel) {

  public static SimulationConfig fromConfigLoader(SimulationConfigLoader configLoader) {
    if (configLoader.packetInjectGap() < 0) {
      throw new IllegalArgumentException("packetInjectGap cannot be negative");
    }

    if (configLoader.linearIncrementalPacketInjection() && configLoader.packetInjectGap() > 0) {
      throw new IllegalArgumentException(
          "choose either linearIncrementalPacketInjection or packetInjectGap, not both");
    }

    if (configLoader.plateauThenLinearPacketInjection() && configLoader.packetInjectGap() > 0) {
      throw new IllegalArgumentException(
          "choose either plateauThenLinearPacketInjection or packetInjectGap, not both");
    }

    if (configLoader.linearIncrementalPacketInjection()
        && configLoader.plateauThenLinearPacketInjection()) {
      throw new IllegalArgumentException(
          "choose either linearIncrementalPacketInjection or plateauThenLinearPacketInjection, not both");
    }

    if (configLoader.totalTicks() < 0 && configLoader.totalPackets() < 0) {
      throw new IllegalArgumentException("either totalTicks or totalPackets must be positive");
    }

    if (configLoader.totalTicks() == 0 && configLoader.totalPackets() == 0) {
      throw new IllegalArgumentException(
          "one of totalTicks or totalPackets must be greater than zero");
    }

    return new SimulationConfig(
        configLoader.topology(),
        configLoader.algorithm().stream()
            .map(String::toUpperCase)
            .map(AlgorithmType::valueOf)
            .toList(),
        configLoader.totalPackets(),
        configLoader.totalTicks(),
        configLoader.packetInjectGap(),
        configLoader.seed(),
        configLoader.metrics().stream().map(String::toUpperCase).map(MetricType::valueOf).toList(),
        configLoader.exportTo().stream().map(String::toUpperCase).map(ExportType::valueOf).toList(),
        configLoader.linearIncrementalPacketInjection(),
        configLoader.plateauThenLinearPacketInjection(),
        configLoader.plateauRampPlateauPacketInjection(),
        configLoader.fixedLoadStepPacketInjection(),
        configLoader.windowedLoadPacketInjection(),
        configLoader.loadLevel());
  }
}
