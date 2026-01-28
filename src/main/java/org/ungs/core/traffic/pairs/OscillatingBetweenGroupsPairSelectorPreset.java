package org.ungs.core.traffic.pairs;

import java.util.List;
import org.ungs.core.config.PairSelectionConfig;
import org.ungs.core.network.Node;
import org.ungs.core.traffic.runtime.TrafficBuildContext;

public final class OscillatingBetweenGroupsPairSelectorPreset implements PairSelectorPreset {

  @Override
  public PairSelectionType type() {
    return PairSelectionType.OSCILLATING_BETWEEN_GROUPS;
  }

  @Override
  public PairSelector create(PairSelectionConfig cfg, TrafficBuildContext ctx) {
    var c = (PairSelectionConfig.OscillatingBetweenGroups) cfg;

    List<Node.Id> a =
        ctx.config().traffic().groups().getOrThrow(c.groupA()).stream().map(Node.Id::new).toList();
    List<Node.Id> b =
        ctx.config().traffic().groups().getOrThrow(c.groupB()).stream().map(Node.Id::new).toList();

    int period = c.periodTicks();

    return runtime -> {
      boolean aToB = ((runtime.getTick() / period) % 2 == 0);
      List<Node.Id> from = aToB ? a : b;
      List<Node.Id> to = aToB ? b : a;

      Node.Id origin = from.get(runtime.getRng().nextIndex(from.size()));
      Node.Id dest;
      do {
        dest = to.get(runtime.getRng().nextIndex(to.size()));
      } while (dest.equals(origin));
      return new NodePair(origin, dest);
    };
  }
}
