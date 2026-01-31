package org.ungs.core.engine;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.network.Network;
import org.ungs.core.network.Node;
import org.ungs.core.network.Packet;
import org.ungs.core.observability.api.EventSink;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.util.DeterministicRng;

public final class SimulationRuntimeContext {
  @Getter private double tick;
  @Getter private DeterministicRng rng;

  @Getter private final SimulationConfigContext config;
  @Getter private final Network network;
  @Getter private AlgorithmType currentAlgorithm;
  @Getter private final List<Packet> notDeliveredPackets;
  @Getter private final List<Packet> deliveredPackets;
  @Getter private final List<PendingSend> pendingSends;

  @Getter private final EventSink eventSink;

  private int nextPacketId;

  public SimulationRuntimeContext(
      SimulationConfigContext config, Network network, EventSink eventSink) {
    this.config = config;
    this.network = network;
    this.rng = new DeterministicRng(config.general().seed());
    this.eventSink = eventSink;
    this.notDeliveredPackets = new ArrayList<>();
    this.deliveredPackets = new ArrayList<>();
    this.pendingSends = new ArrayList<>();
    reset(null);
  }

  public void reset(AlgorithmType algorithm) {
    this.currentAlgorithm = algorithm;
    this.tick = 0.0;
    this.nextPacketId = 0;
    this.rng = new DeterministicRng(config.general().seed());

    this.deliveredPackets.clear();
    this.notDeliveredPackets.clear();
  }

  public void advanceOneTick() {
    tick += 1.0;
  }

  public Packet.Id nextPacketId() {
    return new Packet.Id(nextPacketId++);
  }

  // Phaser: every packet gets sent at the end of the tick
  public List<PendingSend> flushPendingSends() {
    List<PendingSend> toSend = List.copyOf(pendingSends);
    pendingSends.clear();
    return toSend;
  }

  public void schedule(Node.Id from, Node.Id to, Packet packet) {
    pendingSends.add(new PendingSend(from, to, packet));
  }

  public record PendingSend(Node.Id from, Node.Id to, Packet packet) {}
}
