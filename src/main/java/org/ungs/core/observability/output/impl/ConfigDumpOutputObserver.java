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
    Path commonDir = outDir.resolve("common");
    Path configFile = commonDir.resolve("beautified_configuration.json");
    Path rawPropertiesFile = commonDir.resolve("application.properties");

    if (Files.exists(configFile) && Files.exists(rawPropertiesFile)) {
      return;
    }

    try {
      Files.createDirectories(commonDir);

      Files.writeString(configFile, ctx.getConfig().toString());

      try (var in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
        if (in != null) {
          Files.write(rawPropertiesFile, in.readAllBytes());
        } else {
          log.warn("Resource 'application.properties' not found in classpath.");
        }
      }
    } catch (IOException e) {
      log.error("Failed to dump configuration or copy application.properties", e);
    }
  }
}
