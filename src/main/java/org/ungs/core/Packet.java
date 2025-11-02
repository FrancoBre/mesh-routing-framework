package org.ungs.core;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
public class Packet {

  private final Id id;
  private final Node.Id origin;
  private final Node.Id destination;

  private double timeInQueue;

  @Setter private boolean reachedDestination;

  public Packet(Id id, Node.Id origin, Node.Id destination) {
    this.id = id;
    this.origin = origin;
    this.destination = destination;
    this.reachedDestination = false;
    this.timeInQueue = 0.0;
  }

  public void incrementTimeInQueue() {
    this.timeInQueue += 1;
  }

  public record Id(int value) {}
}
