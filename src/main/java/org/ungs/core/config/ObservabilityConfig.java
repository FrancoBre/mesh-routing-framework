package org.ungs.core.config;

import java.util.List;
import org.ungs.cli.SimulationConfigLoader;
import org.ungs.core.metrics.MetricType;
import org.ungs.core.output.OutputType;

public record ObservabilityConfig(
    List<MetricType> metrics,
    List<OutputType> outputs,
    int outputSampleEveryTicks
) {
    public static ObservabilityConfig fromLoader(SimulationConfigLoader l) {
        List<MetricType> metrics = SimulationConfigContext.parseEnumList(l.metrics(), MetricType.class);
        if (metrics.isEmpty()) {
            throw new IllegalArgumentException("metrics must not be empty");
        }
        List<OutputType> outputs = SimulationConfigContext.parseEnumList(l.outputs(), OutputType.class);
        int sampleEvery = l.outputSampleEveryTicks();
        if (sampleEvery <= 0) throw new IllegalArgumentException("output.sample-every-ticks must be > 0");
        return new ObservabilityConfig(metrics, outputs, sampleEvery);
    }
}
