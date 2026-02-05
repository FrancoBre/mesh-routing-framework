package org.ungs.core.observability.events;

import org.ungs.core.observability.api.SimulationEvent;

public record LoadLevelUpdatedEvent(double tick, double loadLevel, LoadLevelTrend trend)
    implements SimulationEvent {

  public LoadLevelUpdatedEvent {
    if (trend == null) {
      trend = LoadLevelTrend.STABLE;
    }
  }

  public enum LoadLevelTrend {
    RISING,
    FALLING,
    STABLE
  }
}
