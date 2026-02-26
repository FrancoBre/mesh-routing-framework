package org.ungs.core.network;

import lombok.Getter;
import lombok.ToString;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.observability.events.PacketDepartedEvent;

@Getter
@ToString
public class Packet {

  private final Id id;
  private final Node.Id origin;
  private final Node.Id destination;

  private double timeInQueue;

  private double departureTime;
  private double arrivalTime;

  public Packet(Id id, Node.Id origin, Node.Id destination) {
    this.id = id;
    this.origin = origin;
    this.destination = destination;
    this.timeInQueue = 0.0;
    this.departureTime = -1.0;
  }

  public void incrementTimeInQueue() {
    this.timeInQueue += 1;
  }

  public void resetTimeInQueue() {
    this.timeInQueue = 0.0;
  }

  public void markAsDeparted(SimulationRuntimeContext ctx) {
    if (departureTime < 0) departureTime = ctx.getTick();
    ctx.getEventSink()
        .emit(
            new PacketDepartedEvent(
                this.getId(), this.getOrigin(), ctx.getTick(), ctx.getCurrentAlgorithm()));
  }

  public void markAsArrived(SimulationRuntimeContext ctx) {
    arrivalTime = ctx.getTick();
  }

  public record Id(int value) {}
}
