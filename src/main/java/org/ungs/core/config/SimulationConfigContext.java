package org.ungs.core.config;

import java.util.*;
import org.ungs.cli.SimulationConfigLoader;

public record SimulationConfigContext(
    GeneralConfig general,
    TerminationConfig termination,
    TrafficConfig traffic,
    NetworkDynamicsConfig dynamics,
    ObservabilityConfig observability
) {

    public static SimulationConfigContext fromLoader(SimulationConfigLoader l) {
        Objects.requireNonNull(l, "configLoader");

        GeneralConfig general = GeneralConfig.fromLoader(l);
        TerminationConfig termination = TerminationConfig.fromLoader(l);
        TrafficConfig traffic = TrafficConfig.fromLoader(l);
        NetworkDynamicsConfig dynamics = NetworkDynamicsConfig.fromLoader(l);
        ObservabilityConfig observability = ObservabilityConfig.fromLoader(l);

        // Cross-section validations
        if (general.warmupTicks() < 0) {
            throw new IllegalArgumentException("warmup-ticks cannot be negative");
        }

        return new SimulationConfigContext(general, termination, traffic, dynamics, observability);
    }

    static Optional<String> rawProperty(SimulationConfigLoader l, String key) {
        return Optional.ofNullable(l.getProperty(key));
    }

    static List<Integer> parseIntCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(Integer::parseInt)
            .toList();
    }

    static OptionalInt parseOptionalInt(String s) {
        if (s == null || s.isBlank()) return OptionalInt.empty();
        return OptionalInt.of(Integer.parseInt(s.trim()));
    }

    static OptionalLong parseOptionalLong(String s) {
        if (s == null || s.isBlank()) return OptionalLong.empty();
        return OptionalLong.of(Long.parseLong(s.trim()));
    }

    static <E extends Enum<E>> List<E> parseEnumList(List<String> raw, Class<E> enumClass) {
        if (raw == null) return List.of();
        return raw.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> Enum.valueOf(enumClass, s.toUpperCase(Locale.ROOT)))
            .toList();
    }

    static <E extends Enum<E>> E parseEnum(String raw, Class<E> enumClass) {
        return Enum.valueOf(enumClass, raw.trim().toUpperCase(Locale.ROOT));
    }
}
