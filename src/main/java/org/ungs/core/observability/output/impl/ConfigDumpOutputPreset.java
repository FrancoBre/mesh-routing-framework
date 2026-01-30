package org.ungs.core.observability.output.impl;

import java.nio.file.Path;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.network.Network;
import org.ungs.core.observability.output.api.OutputBundle;
import org.ungs.core.observability.output.api.OutputPreset;
import org.ungs.core.observability.output.api.OutputType;
import org.ungs.core.observability.route.RouteRecorderObserver;

public class ConfigDumpOutputPreset implements OutputPreset {

  @Override
  public OutputType type() {
    return OutputType.CONFIG_DUMP;
  }

  @Override
  public OutputBundle createBundle(
      SimulationConfigContext simCfg, Network network, RouteRecorderObserver route, Path outDir) {
    return new OutputBundle(OutputType.CONFIG_DUMP.name(), new ConfigDumpOutputObserver(outDir));
  }
}
