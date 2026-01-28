package org.ungs.core.traffic.pairs;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.ungs.core.config.PairConstraintsConfig;

@UtilityClass
public final class PairConstraintsFactory {

  public static List<PairConstraint> from(PairConstraintsConfig cfg) {
    List<PairConstraint> constraints = new ArrayList<>();
    if (cfg.disallowSelf())
      constraints.add((ctx, pair) -> !pair.origin().equals(pair.destination()));
    if (cfg.disallowNeighbor())
      constraints.add(
          (ctx, pair) -> !ctx.getNetwork().isNeighbor(pair.destination(), pair.origin()));
    return constraints;
  }
}
