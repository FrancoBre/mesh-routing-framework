package org.ungs.core.traffic.pairs;

import java.util.List;
import org.ungs.core.config.PairSelectionConfig;
import org.ungs.core.network.Node;
import org.ungs.core.traffic.runtime.TrafficBuildContext;

public final class RandomPairSelectorPreset implements PairSelectorPreset {

  @Override
  public PairSelectionType type() {
    return PairSelectionType.RANDOM;
  }

  @Override
  public PairSelector create(PairSelectionConfig cfg, TrafficBuildContext ctx) {
    List<Node.Id> nodeIds = ctx.stableNodeIds();

    return runtime -> {
      Node.Id origin = nodeIds.get(runtime.getRng().nextIndex(nodeIds.size()));
      Node.Id dest;
      do {
        dest = nodeIds.get(runtime.getRng().nextIndex(nodeIds.size()));
      } while (dest.equals(origin));
      return new NodePair(origin, dest);
    };
  }
}
