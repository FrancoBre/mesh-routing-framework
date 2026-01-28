package org.ungs.core.traffic.pairs;

import org.ungs.core.network.Node;

public record NodePair(Node.Id origin, Node.Id destination) {}
