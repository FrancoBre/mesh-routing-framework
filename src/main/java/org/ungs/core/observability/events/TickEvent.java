package org.ungs.core.observability.events;

import org.ungs.core.observability.api.SimulationEvent;
import org.ungs.core.routing.api.AlgorithmType;

public record TickEvent(
    double tick,
    AlgorithmType algorithm,
    int packetsInFlight,
    long deliveredCount,
    long sentThisTick)
    implements SimulationEvent {}
