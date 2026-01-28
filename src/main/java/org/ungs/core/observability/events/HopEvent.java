package org.ungs.core.observability.events;

import org.ungs.core.network.Node;
import org.ungs.core.network.Packet;
import org.ungs.core.observability.api.SimulationEvent;
import org.ungs.core.routing.api.AlgorithmType;

public record HopEvent(
    Packet.Id packetId,
    Node.Id from,
    Node.Id to,
    long sentTick,
    long expectedReceiveTick,
    AlgorithmType algorithm)
    implements SimulationEvent {}
