package org.ungs.cli;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.aeonbits.owner.ConfigFactory;
import org.ungs.core.Registry;
import org.ungs.core.Simulation;
import org.ungs.core.SimulationConfig;
import org.ungs.core.TopologyLoader;
import org.ungs.metrics.MetricLoader;

@Slf4j
public class Main {

  public static void main(String[] args) throws IOException {

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

    // parse args
    var configLoader = ConfigFactory.create(SimulationConfigLoader.class);
    var config = SimulationConfig.fromConfigLoader(configLoader);

    log.info("Simulation configuration ready: {}", config);

    // build network
    var network = TopologyLoader.createNetwork(config.topology());

    log.info("Network created");

    // bootstrap metrics
    var metrics = MetricLoader.createMetrics(config.metrics());

    // add metrics to registry
    Registry.getInstance().setMetrics(metrics);
    Registry.getInstance().setNetwork(network);

    // start simulation
    var simulation = new Simulation(config, network);
    simulation.run();

    log.info("Simulation finished");
  }
}
