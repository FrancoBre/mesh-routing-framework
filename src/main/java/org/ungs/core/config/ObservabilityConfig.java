package org.ungs.core.config;

import static org.ungs.core.observability.output.api.OutputType.GIF_ROUTE;
import static org.ungs.core.observability.output.api.OutputType.ROUTE_FRAMES;

import java.util.List;
import org.ungs.cli.SimulationConfigLoader;
import org.ungs.core.observability.metrics.api.MetricType;
import org.ungs.core.observability.output.api.OutputType;

public record ObservabilityConfig(
    List<MetricType> metrics, List<OutputType> outputs, int outputSampleEveryTicks) {

  public static ObservabilityConfig fromLoader(SimulationConfigLoader l) {
    List<MetricType> metrics = SimulationConfigContext.parseEnumList(l.metrics(), MetricType.class);

    if (metrics.isEmpty()) {
      throw new IllegalArgumentException("metrics must not be empty");
    }

    List<OutputType> outputs = SimulationConfigContext.parseEnumList(l.outputs(), OutputType.class);

    int sampleEvery = l.outputSampleEveryTicks();
    if (sampleEvery <= 0)
      throw new IllegalArgumentException("output.sample-every-ticks must be > 0");

    if (outputs.contains(GIF_ROUTE) && !outputs.contains(ROUTE_FRAMES))
      throw new IllegalArgumentException(
          "GIF_ROUTE output requires ROUTE_FRAMES output to be enabled");

    return new ObservabilityConfig(metrics, outputs, sampleEvery);
  }
}
