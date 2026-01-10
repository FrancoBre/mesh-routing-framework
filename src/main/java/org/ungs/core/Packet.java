package org.ungs.core;

import lombok.Getter;
import lombok.ToString;

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

  public void markAsDeparted() {
    if (departureTime < 0) departureTime = Simulation.TIME;
  }

  public void markAsReceived() {
    this.arrivalTime = Simulation.TIME;
  }

  public double getDeliveryTime() {
    return this.getArrivalTime() - this.getDepartureTime();
  }

  public record Id(int value) {}
}
