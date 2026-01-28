package org.ungs.core.traffic.runtime;

import java.util.List;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Packet;
import org.ungs.core.traffic.pairs.NodePair;
import org.ungs.core.traffic.pairs.PairConstraint;
import org.ungs.core.traffic.pairs.PairSelector;
import org.ungs.core.traffic.schedule.InjectionSchedule;

public final class TrafficInjector {
  private final InjectionSchedule schedule;
  private final PairSelector pairSelector;
  private final List<PairConstraint> constraints;
  private final int maxActivePackets;

  public TrafficInjector(
      InjectionSchedule schedule,
      PairSelector pairSelector,
      List<PairConstraint> constraints,
      int maxActivePackets) {
    this.schedule = schedule;
    this.pairSelector = pairSelector;
    this.constraints = constraints;
    this.maxActivePackets = maxActivePackets;
  }

  public void inject(SimulationRuntimeContext ctx) {
    int injectCount = schedule.packetsToInject(ctx);

    int availableSlots = maxActivePackets - ctx.getNetwork().packetsInFlight();
    if (availableSlots <= 0) return;

    injectCount = Math.min(injectCount, availableSlots);

    for (int i = 0; i < injectCount; i++) {
      NodePair pair = pairSelector.pickPair(ctx);

      boolean ok = true;
      for (PairConstraint c : constraints) {
        if (!c.isAllowed(ctx, pair)) {
          ok = false;
          break;
        }
      }
      if (!ok) continue;

      Packet packet = new Packet(ctx.nextPacketId(), pair.origin(), pair.destination());
      packet.markAsDeparted(ctx);
      ctx.getNetwork().getNode(pair.origin()).receivePacket(packet);
    }
  }
}
