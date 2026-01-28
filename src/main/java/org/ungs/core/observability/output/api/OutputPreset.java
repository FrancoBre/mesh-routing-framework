package org.ungs.core.observability.output.api;

import java.nio.file.Path;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.network.Network;
import org.ungs.core.observability.route.RouteRecorderObserver;

public interface OutputPreset {

  OutputType type();

  OutputBundle createBundle(
      SimulationConfigContext simCfg, Network network, RouteRecorderObserver route, Path outDir);
}
