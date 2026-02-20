package org.ungs.core.config;

import java.util.List;
import org.ungs.cli.SimulationConfigLoader;
import org.ungs.core.dynamics.api.NetworkDynamicsType;

public sealed interface NetworkDynamicsConfig
    permits NetworkDynamicsConfig.None,
        NetworkDynamicsConfig.NodeFailures,
        NetworkDynamicsConfig.ScheduledLinkFailures {

  NetworkDynamicsType type();

  record None() implements NetworkDynamicsConfig {
    @Override
    public NetworkDynamicsType type() {
      return NetworkDynamicsType.NONE;
    }
  }

  record NodeFailures(String model, double p, int meanDowntimeTicks, int meanUptimeTicks)
      implements NetworkDynamicsConfig {
    @Override
    public NetworkDynamicsType type() {
      return NetworkDynamicsType.NODE_FAILURES;
    }
  }

  /**
   * Configuration for scheduled link disconnections.
   *
   * @param disconnectAtTick tick at which to disconnect the specified links
   * @param reconnectAtTick tick at which to reconnect the links (0 or negative = never reconnect)
   * @param links list of link specifications in format "nodeA-nodeB" (e.g., "8-9", "14-15")
   */
  record ScheduledLinkFailures(
      @com.fasterxml.jackson.annotation.JsonProperty("disconnect_at_tick") int disconnectAtTick,
      @com.fasterxml.jackson.annotation.JsonProperty("reconnect_at_tick") int reconnectAtTick,
      @com.fasterxml.jackson.annotation.JsonProperty("links") List<LinkSpec> links)
      implements NetworkDynamicsConfig {
    @Override
    public NetworkDynamicsType type() {
      return NetworkDynamicsType.SCHEDULED_LINK_FAILURES;
    }

    public record LinkSpec(
        @com.fasterxml.jackson.annotation.JsonProperty("node_a") int nodeA,
        @com.fasterxml.jackson.annotation.JsonProperty("node_b") int nodeB) {
      public static LinkSpec parse(String spec) {
        String[] parts = spec.trim().split("-");
        if (parts.length != 2) {
          throw new IllegalArgumentException(
              "Invalid link specification: '"
                  + spec
                  + "'. Expected format: 'nodeA-nodeB' (e.g., '8-9')");
        }
        return new LinkSpec(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
      }
    }
  }

  static NetworkDynamicsConfig fromLoader(SimulationConfigLoader l) {
    NetworkDynamicsType type =
        SimulationConfigContext.parseEnum(l.networkDynamics(), NetworkDynamicsType.class);

    return switch (type) {
      case NONE -> new None();
      case NODE_FAILURES -> {
        double p = l.nodeFailuresRandomP();
        if (p < 0.0 || p > 1.0)
          throw new IllegalArgumentException(
              "network-dynamics.node-failures.random.p must be in [0,1]");
        int down = l.nodeFailuresMeanDowntimeTicks();
        int up = l.nodeFailuresMeanUptimeTicks();
        if (down <= 0 || up <= 0)
          throw new IllegalArgumentException("mean downtime/uptime must be > 0");
        yield new NodeFailures(l.nodeFailuresModel(), p, down, up);
      }
      case SCHEDULED_LINK_FAILURES -> {
        int disconnectAt = l.scheduledLinkFailuresDisconnectAtTick();
        int reconnectAt = l.scheduledLinkFailuresReconnectAtTick();
        List<String> linksRaw = l.scheduledLinkFailuresLinks();
        if (linksRaw == null || linksRaw.isEmpty()) {
          throw new IllegalArgumentException(
              "network-dynamics.scheduled-link-failures.links must not be empty");
        }
        List<ScheduledLinkFailures.LinkSpec> links =
            linksRaw.stream().map(ScheduledLinkFailures.LinkSpec::parse).toList();
        yield new ScheduledLinkFailures(disconnectAt, reconnectAt, links);
      }
      case MOBILITY ->
          throw new IllegalArgumentException(
              "network-dynamics=" + type + " is not implemented yet");
    };
  }
}
