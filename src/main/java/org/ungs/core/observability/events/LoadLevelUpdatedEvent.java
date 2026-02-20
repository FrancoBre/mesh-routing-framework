package org.ungs.core.observability.events;

import org.ungs.core.observability.api.SimulationEvent;

public record LoadLevelUpdatedEvent(double tick, double loadLevel, LoadLevelTrend trend)
    implements SimulationEvent {

  public LoadLevelUpdatedEvent {
    if (trend == null) {
      trend = LoadLevelTrend.PLATEAU;
    }
  }

  public enum LoadLevelTrend {
    RISING,
    FALLING,
    PLATEAU
  }
}
