package org.ungs.core.observability.events;

import org.ungs.core.network.Packet;
import org.ungs.core.observability.api.SimulationEvent;
import org.ungs.core.routing.api.AlgorithmType;

public record PacketDeliveredEvent(
    Packet packet, int pathHopCount, double receivedTime, AlgorithmType algorithm)
    implements SimulationEvent {}
