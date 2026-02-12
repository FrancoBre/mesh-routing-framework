package org.ungs.core.observability.api;

import java.util.Map;
import org.ungs.core.network.Node;
import org.ungs.core.routing.api.AlgorithmType;

public record QTableSnapshotEvent(
    double tick,
    AlgorithmType algorithm,
    Node.Id node,
    Node.Id destination,
    Map<Node.Id, Double> qByNeighbor)
    implements SimulationEvent {}
