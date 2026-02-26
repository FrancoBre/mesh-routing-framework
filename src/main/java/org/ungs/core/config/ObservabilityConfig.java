package org.ungs.core.config;

import static org.ungs.core.observability.metrics.api.MetricType.AVG_DELIVERY_TIME_VS_LOAD_LEVEL;
import static org.ungs.core.observability.output.api.OutputType.GIF_ROUTE;
import static org.ungs.core.observability.output.api.OutputType.ROUTE_FRAMES;
import static org.ungs.core.traffic.schedule.InjectionScheduleType.LINEAR_LOAD_LEVEL;
import static org.ungs.core.traffic.schedule.InjectionScheduleType.LOAD_LEVEL;
import static org.ungs.core.traffic.schedule.InjectionScheduleType.TRIANGULAR_LOAD_LEVEL;

import java.util.List;
import java.util.OptionalLong;
import org.ungs.cli.SimulationConfigLoader;
import org.ungs.core.observability.metrics.api.MetricType;
import org.ungs.core.observability.output.api.OutputType;
import org.ungs.core.traffic.schedule.InjectionScheduleType;

public record ObservabilityConfig(
    List<MetricType> metrics,
    List<OutputType> outputs,
    int outputSampleEveryTicks,
    int metricWindowSize,
    long heatmapFromTick,
    OptionalLong heatmapToTick) {

  public static ObservabilityConfig fromLoader(SimulationConfigLoader l) {
    List<MetricType> metrics = SimulationConfigContext.parseEnumList(l.metrics(), MetricType.class);

    if (metrics.isEmpty()) {
      throw new IllegalArgumentException("metrics must not be empty");
    }

    InjectionScheduleType type =
        SimulationConfigContext.parseEnum(l.injectionSchedule(), InjectionScheduleType.class);
    if (metrics.contains(AVG_DELIVERY_TIME_VS_LOAD_LEVEL)
        && !type.equals(LOAD_LEVEL)
        && !type.equals(TRIANGULAR_LOAD_LEVEL)
        && !type.equals(LINEAR_LOAD_LEVEL)) {
      throw new IllegalArgumentException(
          "AVG_DELIVERY_TIME_VS_LOAD_LEVEL metric requires injection schedule of type LOAD_LEVEL or TRIANGULAR_LOAD_LEVEL");
    }

    List<OutputType> outputs = SimulationConfigContext.parseEnumList(l.outputs(), OutputType.class);

    int sampleEvery = l.outputSampleEveryTicks();
    if (sampleEvery <= 0)
      throw new IllegalArgumentException("output.sample-every-ticks must be > 0");

    int metricWindowSize = l.metricWindowSize();
    if (metricWindowSize < 0)
      throw new IllegalArgumentException(
          "metric.window-size must be >= 0 (0 = disabled/cumulative)");

    if (outputs.contains(GIF_ROUTE) && !outputs.contains(ROUTE_FRAMES))
      throw new IllegalArgumentException(
          "GIF_ROUTE output requires ROUTE_FRAMES output to be enabled");

    long heatmapFromTick = l.heatmapFromTick();
    if (heatmapFromTick < 0) {
      throw new IllegalArgumentException("output.heatmap.from-tick must be >= 0");
    }

    OptionalLong heatmapToTick = parseOptionalLong(l.heatmapToTick());
    if (heatmapToTick.isPresent() && heatmapToTick.getAsLong() <= heatmapFromTick) {
      throw new IllegalArgumentException(
          "output.heatmap.to-tick must be > output.heatmap.from-tick when set");
    }

    return new ObservabilityConfig(
        metrics, outputs, sampleEvery, metricWindowSize, heatmapFromTick, heatmapToTick);
  }

  private static OptionalLong parseOptionalLong(String s) {
    if (s == null || s.isBlank()) return OptionalLong.empty();
    try {
      return OptionalLong.of(Long.parseLong(s.trim()));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid long value: " + s);
    }
  }
}
