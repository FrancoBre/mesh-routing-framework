package org.ungs.core.traffic.runtime;

import java.util.Comparator;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.network.Network;
import org.ungs.core.network.Node;
import org.ungs.core.traffic.pairs.PairConstraintsFactory;
import org.ungs.core.traffic.pairs.PairSelector;
import org.ungs.core.traffic.pairs.PairSelectorFactory;
import org.ungs.core.traffic.schedule.InjectionSchedule;
import org.ungs.core.traffic.schedule.InjectionScheduleFactory;

@UtilityClass
public final class TrafficFactory {

  public static TrafficInjector from(SimulationConfigContext cfg, Network network) {

    // stable node ids
    List<Node.Id> nodeIds =
        network.getNodes().stream()
            .map(Node::getId)
            .sorted(Comparator.comparingInt(Node.Id::value))
            .toList();

    TrafficBuildContext buildCtx = new TrafficBuildContext(cfg, network, nodeIds);

    InjectionSchedule schedule = InjectionScheduleFactory.from(cfg.traffic().injectionSchedule());
    PairSelector selector = PairSelectorFactory.from(cfg.traffic().pairSelection(), buildCtx);

    var constraints = PairConstraintsFactory.from(cfg.traffic().constraints());

    int maxActive = cfg.general().maxActivePackets().orElse(Integer.MAX_VALUE);

    return new TrafficInjector(schedule, selector, constraints, maxActive);
  }
}
