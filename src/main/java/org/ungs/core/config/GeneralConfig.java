package org.ungs.core.config;

import java.util.List;
import java.util.OptionalInt;
import org.ungs.cli.SimulationConfigLoader;
import org.ungs.core.TopologyType;
import org.ungs.core.routing.AlgorithmType;

public record GeneralConfig(
    long seed,
    TopologyType topology,
    String topologyFile,
    List<AlgorithmType> algorithms,
    OptionalInt maxActivePackets,
    int warmupTicks,
    String experimentName,
    String outputFolder
) {

    public static GeneralConfig fromLoader(SimulationConfigLoader l) {
        TopologyType topology = SimulationConfigContext.parseEnum(l.topology(), TopologyType.class);

        String topologyFile = l.topologyFile();
        if (topology == TopologyType.FILE && (topologyFile == null || topologyFile.isBlank())) {
            throw new IllegalArgumentException("topology.file is mandatory when topology=FILE");
        }

        List<AlgorithmType> algorithms =
            SimulationConfigContext.parseEnumList(l.algorithms(), AlgorithmType.class);
        if (algorithms.isEmpty()) {
            throw new IllegalArgumentException("algorithms must not be empty");
        }

        OptionalInt maxActivePackets = SimulationConfigContext.parseOptionalInt(l.maxActivePackets());
        if (maxActivePackets.isPresent() && maxActivePackets.getAsInt() <= 0) {
            throw new IllegalArgumentException("max-active-packets must be > 0 when set");
        }

        int warmupTicks = l.warmupTicks();
        if (warmupTicks < 0) {
            throw new IllegalArgumentException("warmup-ticks cannot be negative");
        }

        String outputFolder = (l.outputFolder() == null || l.outputFolder().isBlank())
            ? "./results/"
            : l.outputFolder().trim();

        String experimentName = (l.experimentName() == null) ? "" : l.experimentName().trim();

        return new GeneralConfig(
            l.seed(),
            topology,
            topologyFile,
            algorithms,
            maxActivePackets,
            warmupTicks,
            experimentName,
            outputFolder
        );
    }
}
