package org.ungs.core.traffic.schedule;

import org.ungs.core.engine.SimulationRuntimeContext;

public interface InjectionSchedule {

  int packetsToInject(SimulationRuntimeContext ctx);
}
