package org.ungs.core.observability.output.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.observability.api.SimulationObserver;

@Slf4j
public record ConfigDumpOutputObserver(Path outDir) implements SimulationObserver {

  public void onSimulationStart(SimulationRuntimeContext ctx) {

    Path configFile = outDir.resolve("configuration.txt");

    if (Files.exists(configFile)) {
      return;
    }

    try {
      Files.createDirectories(configFile.getParent());
      Files.writeString(configFile, ctx.toString());
    } catch (IOException e) {
      log.error("Could not create directories for config dump observer", e);
    }
  }
}
