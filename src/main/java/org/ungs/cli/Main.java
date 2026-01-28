package org.ungs.cli;

import lombok.extern.slf4j.Slf4j;
import org.aeonbits.owner.ConfigFactory;
import org.ungs.core.config.SimulationConfigContext;
import org.ungs.core.engine.SimulationEngine;
import org.ungs.core.topology.factory.TopologyFactory;

@Slf4j
public class Main {

  public static void main(String[] args) {

    log.info(
        """
            \u001B[0m\
            \n _      _____ ____  _       ____  ____  _    _____ _  _      _____   _____ ____  ____  _      _____ _      ____  ____  _  __
            / \\__/|/  __// ___\\/ \\ /|  /  __\\/  _ \\/ \\ /Y__ __Y \\/ \\  /|/  __/  /    //  __\\/  _ \\/ \\__/|/  __// \\  /|/  _ \\/  __\\/ |/ /
            | |\\/|||  \\  |    \\| |_||  |  \\/|| / \\|| | || / \\ | || |\\ ||| |  _  |  __\\|  \\/|| / \\|| |\\/|||  \\  | |  ||| / \\||  \\/||   /\s
            | |  |||  /_ \\___ || | ||  |    /| \\_/|| \\_/| | | | || | \\||| |_//  | |   |    /| |-||| |  |||  /_ | |/\\||| \\_/||    /|   \\\s
            \\_/  \\|\\____\\\\____/\\_/ \\|  \\_/\\_\\\\____/\\____/ \\_/ \\_/\\_/  \\|\\____\\  \\_/   \\_/\\_\\\\_/ \\|\\_/  \\|\\____\\\\_/  \\|\\____/\\_/\\_\\\\_|\\_\\
            \u001B[36m
            """);

    var loader = ConfigFactory.create(SimulationConfigLoader.class);
    var configCtx = SimulationConfigContext.fromLoader(loader);

    var network = TopologyFactory.createNetwork(configCtx.general().topology());

    new SimulationEngine(configCtx, network).run();
  }
}
