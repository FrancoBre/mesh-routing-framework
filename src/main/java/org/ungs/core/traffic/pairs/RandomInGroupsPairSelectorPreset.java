package org.ungs.core.traffic.pairs;

import java.util.List;
import org.ungs.core.config.PairSelectionConfig;
import org.ungs.core.network.Node;
import org.ungs.core.traffic.runtime.TrafficBuildContext;

public final class RandomInGroupsPairSelectorPreset implements PairSelectorPreset {

  @Override
  public PairSelectionType type() {
    return PairSelectionType.RANDOM_IN_GROUPS;
  }

  @Override
  public PairSelector create(PairSelectionConfig cfg, TrafficBuildContext ctx) {
    var c = (PairSelectionConfig.RandomInGroups) cfg;

    List<Integer> fromIds = ctx.config().traffic().groups().getOrThrow(c.fromGroup());
    List<Integer> toIds = ctx.config().traffic().groups().getOrThrow(c.toGroup());

    // map int -> Node.Id
    List<Node.Id> from = fromIds.stream().map(Node.Id::new).toList();
    List<Node.Id> to = toIds.stream().map(Node.Id::new).toList();

    return runtime -> {
      Node.Id origin = from.get(runtime.getRng().nextIndex(from.size()));
      Node.Id dest;
      do {
        dest = to.get(runtime.getRng().nextIndex(to.size()));
      } while (dest.equals(origin));
      return new NodePair(origin, dest);
    };
  }
}
