package org.ungs.core.observability.events;

import org.ungs.core.network.Node;
import org.ungs.core.network.Packet;
import org.ungs.core.observability.api.SimulationEvent;
import org.ungs.core.routing.api.AlgorithmType;

public record PacketDepartedEvent(
    Packet.Id packetId, Node.Id from, double departedTick, AlgorithmType algorithm)
    implements SimulationEvent {}
