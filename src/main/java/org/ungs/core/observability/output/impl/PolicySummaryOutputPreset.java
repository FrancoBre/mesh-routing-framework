package org.ungs.core.observability.output.impl;

import java.nio.file.Path;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.network.Network;
import org.ungs.core.observability.output.api.OutputBundle;
import org.ungs.core.observability.output.api.OutputPreset;
import org.ungs.core.observability.output.api.OutputType;
import org.ungs.core.observability.route.RouteRecorderObserver;

public final class PolicySummaryOutputPreset implements OutputPreset {

  @Override
  public OutputType type() {
    return OutputType.POLICY_SUMMARY;
  }

  @Override
  public OutputBundle createBundle(
      SimulationConfigContext simCfg, Network network, RouteRecorderObserver route, Path outDir) {
    return new OutputBundle(
        OutputType.POLICY_SUMMARY.name(), new PolicySummaryOutputObserver(network, outDir));
  }
}
