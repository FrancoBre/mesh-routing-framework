package org.ungs.testutil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.observability.api.ObserverHub;
import org.ungs.core.observability.api.SimulationEvent;
import org.ungs.core.observability.events.HopEvent;
import org.ungs.core.observability.events.PacketDeliveredEvent;
import org.ungs.core.observability.events.PacketDepartedEvent;

public class MockEventSink implements ObserverHub {

  private final List<SimulationEvent> events = new ArrayList<>();
  @Getter private int algorithmStartCount = 0;
  @Getter private int algorithmEndCount = 0;

  @Override
  public void emit(SimulationEvent event) {
    events.add(event);
  }

  @Override
  public void onSimulationStart(SimulationRuntimeContext ctx) {}

  @Override
  public void onAlgorithmStart(SimulationRuntimeContext ctx) {
    algorithmStartCount++;
  }

  @Override
  public void onAlgorithmEnd(SimulationRuntimeContext ctx) {
    algorithmEndCount++;
  }

  @Override
  public void onSimulationEnd(SimulationRuntimeContext ctx) {}

  // Query methods for assertions

  public <T extends SimulationEvent> List<T> getEventsOfType(Class<T> eventType) {
    return events.stream()
        .filter(eventType::isInstance)
        .map(eventType::cast)
        .collect(Collectors.toList());
  }

  public List<HopEvent> getHopEvents() {
    return getEventsOfType(HopEvent.class);
  }

  public List<PacketDeliveredEvent> getDeliveredEvents() {
    return getEventsOfType(PacketDeliveredEvent.class);
  }

  public List<PacketDepartedEvent> getDepartedEvents() {
    return getEventsOfType(PacketDepartedEvent.class);
  }
}
