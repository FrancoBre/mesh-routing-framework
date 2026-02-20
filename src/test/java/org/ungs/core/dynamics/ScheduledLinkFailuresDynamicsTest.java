package org.ungs.core.dynamics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ungs.core.config.NetworkDynamicsConfig;
import org.ungs.core.dynamics.impl.ScheduledLinkFailuresDynamics;
import org.ungs.core.engine.SimulationRuntimeContext;
import org.ungs.core.network.Network;
import org.ungs.core.network.Node;
import org.ungs.core.routing.api.AlgorithmType;
import org.ungs.testutil.MockEventSink;
import org.ungs.testutil.TestConfigBuilder;
import org.ungs.testutil.TestNetworkBuilder;

@DisplayName("ScheduledLinkFailuresDynamics")
class ScheduledLinkFailuresDynamicsTest {

  private Network network;
  private SimulationRuntimeContext ctx;

  @BeforeEach
  void setUp() {
    network = TestNetworkBuilder.linearChain(5); // 0 - 1 - 2 - 3 - 4
    MockEventSink eventSink = new MockEventSink();
    ctx = new SimulationRuntimeContext(TestConfigBuilder.withSeed(42L), network, eventSink);
    ctx.reset(AlgorithmType.Q_ROUTING);
    network.setRuntimeContext(ctx);
  }

  @Nested
  @DisplayName("Link Disconnection")
  class LinkDisconnection {

    @Test
    @DisplayName("should disconnect link at specified tick")
    void disconnectsAtSpecifiedTick() {
      NetworkDynamicsConfig.ScheduledLinkFailures config =
          new NetworkDynamicsConfig.ScheduledLinkFailures(
              5, // disconnect at tick 5
              0, // never reconnect
              List.of(new NetworkDynamicsConfig.ScheduledLinkFailures.LinkSpec(1, 2)));

      ScheduledLinkFailuresDynamics dynamics = new ScheduledLinkFailuresDynamics(config);

      // Before tick 5, nodes 1 and 2 should be neighbors
      assertTrue(network.isNeighbor(new Node.Id(1), new Node.Id(2)));

      // Advance to tick 5
      for (int i = 0; i < 5; i++) {
        ctx.advanceOneTick();
      }

      dynamics.beforeTick(ctx);

      // After tick 5, nodes 1 and 2 should not be neighbors
      assertFalse(network.isNeighbor(new Node.Id(1), new Node.Id(2)));
    }

    @Test
    @DisplayName("should disconnect multiple links")
    void disconnectsMultipleLinks() {
      NetworkDynamicsConfig.ScheduledLinkFailures config =
          new NetworkDynamicsConfig.ScheduledLinkFailures(
              3,
              0,
              List.of(
                  new NetworkDynamicsConfig.ScheduledLinkFailures.LinkSpec(0, 1),
                  new NetworkDynamicsConfig.ScheduledLinkFailures.LinkSpec(2, 3)));

      ScheduledLinkFailuresDynamics dynamics = new ScheduledLinkFailuresDynamics(config);

      // Verify both links exist
      assertTrue(network.isNeighbor(new Node.Id(0), new Node.Id(1)));
      assertTrue(network.isNeighbor(new Node.Id(2), new Node.Id(3)));

      // Advance to tick 3
      for (int i = 0; i < 3; i++) {
        ctx.advanceOneTick();
      }

      dynamics.beforeTick(ctx);

      // Both links should be disconnected
      assertFalse(network.isNeighbor(new Node.Id(0), new Node.Id(1)));
      assertFalse(network.isNeighbor(new Node.Id(2), new Node.Id(3)));
    }

    @Test
    @DisplayName("should disconnect bidirectionally")
    void disconnectsBidirectionally() {
      NetworkDynamicsConfig.ScheduledLinkFailures config =
          new NetworkDynamicsConfig.ScheduledLinkFailures(
              2, 0, List.of(new NetworkDynamicsConfig.ScheduledLinkFailures.LinkSpec(1, 2)));

      ScheduledLinkFailuresDynamics dynamics = new ScheduledLinkFailuresDynamics(config);

      for (int i = 0; i < 2; i++) {
        ctx.advanceOneTick();
      }
      dynamics.beforeTick(ctx);

      Node node1 = network.getNode(new Node.Id(1));
      Node node2 = network.getNode(new Node.Id(2));

      assertFalse(node1.getNeighbors().contains(node2));
      assertFalse(node2.getNeighbors().contains(node1));
    }

    @Test
    @DisplayName("should not disconnect before specified tick")
    void doesNotDisconnectEarly() {
      NetworkDynamicsConfig.ScheduledLinkFailures config =
          new NetworkDynamicsConfig.ScheduledLinkFailures(
              10, 0, List.of(new NetworkDynamicsConfig.ScheduledLinkFailures.LinkSpec(1, 2)));

      ScheduledLinkFailuresDynamics dynamics = new ScheduledLinkFailuresDynamics(config);

      // At tick 5 (before disconnect tick)
      for (int i = 0; i < 5; i++) {
        ctx.advanceOneTick();
      }
      dynamics.beforeTick(ctx);

      // Link should still exist
      assertTrue(network.isNeighbor(new Node.Id(1), new Node.Id(2)));
    }
  }

  @Nested
  @DisplayName("Link Reconnection")
  class LinkReconnection {

    @Test
    @DisplayName("should reconnect link at specified tick")
    void reconnectsAtSpecifiedTick() {
      NetworkDynamicsConfig.ScheduledLinkFailures config =
          new NetworkDynamicsConfig.ScheduledLinkFailures(
              3, // disconnect at tick 3
              7, // reconnect at tick 7
              List.of(new NetworkDynamicsConfig.ScheduledLinkFailures.LinkSpec(1, 2)));

      ScheduledLinkFailuresDynamics dynamics = new ScheduledLinkFailuresDynamics(config);

      // Advance to tick 3 and disconnect
      for (int i = 0; i < 3; i++) {
        ctx.advanceOneTick();
      }
      dynamics.beforeTick(ctx);
      assertFalse(network.isNeighbor(new Node.Id(1), new Node.Id(2)));

      // Advance to tick 7 and reconnect
      for (int i = 0; i < 4; i++) {
        ctx.advanceOneTick();
      }
      dynamics.beforeTick(ctx);
      assertTrue(network.isNeighbor(new Node.Id(1), new Node.Id(2)));
    }

    @Test
    @DisplayName("should not reconnect when reconnect tick is 0")
    void doesNotReconnectWhenTickIsZero() {
      NetworkDynamicsConfig.ScheduledLinkFailures config =
          new NetworkDynamicsConfig.ScheduledLinkFailures(
              3,
              0, // never reconnect
              List.of(new NetworkDynamicsConfig.ScheduledLinkFailures.LinkSpec(1, 2)));

      ScheduledLinkFailuresDynamics dynamics = new ScheduledLinkFailuresDynamics(config);

      // Disconnect
      for (int i = 0; i < 3; i++) {
        ctx.advanceOneTick();
      }
      dynamics.beforeTick(ctx);
      assertFalse(network.isNeighbor(new Node.Id(1), new Node.Id(2)));

      // Advance many ticks
      for (int i = 0; i < 100; i++) {
        ctx.advanceOneTick();
        dynamics.beforeTick(ctx);
      }

      // Should still be disconnected
      assertFalse(network.isNeighbor(new Node.Id(1), new Node.Id(2)));
    }

    @Test
    @DisplayName("should reconnect bidirectionally")
    void reconnectsBidirectionally() {
      NetworkDynamicsConfig.ScheduledLinkFailures config =
          new NetworkDynamicsConfig.ScheduledLinkFailures(
              2, 5, List.of(new NetworkDynamicsConfig.ScheduledLinkFailures.LinkSpec(1, 2)));

      ScheduledLinkFailuresDynamics dynamics = new ScheduledLinkFailuresDynamics(config);

      // Disconnect
      for (int i = 0; i < 2; i++) {
        ctx.advanceOneTick();
      }
      dynamics.beforeTick(ctx);

      // Reconnect
      for (int i = 0; i < 3; i++) {
        ctx.advanceOneTick();
      }
      dynamics.beforeTick(ctx);

      Node node1 = network.getNode(new Node.Id(1));
      Node node2 = network.getNode(new Node.Id(2));

      assertTrue(node1.getNeighbors().contains(node2));
      assertTrue(node2.getNeighbors().contains(node1));
    }
  }

  @Nested
  @DisplayName("Algorithm End Behavior")
  class AlgorithmEndBehavior {

    @Test
    @DisplayName("should restore links on algorithm end")
    void restoresLinksOnAlgorithmEnd() {
      NetworkDynamicsConfig.ScheduledLinkFailures config =
          new NetworkDynamicsConfig.ScheduledLinkFailures(
              2,
              0, // never reconnect during run
              List.of(new NetworkDynamicsConfig.ScheduledLinkFailures.LinkSpec(1, 2)));

      ScheduledLinkFailuresDynamics dynamics = new ScheduledLinkFailuresDynamics(config);

      // Disconnect link
      for (int i = 0; i < 2; i++) {
        ctx.advanceOneTick();
      }
      dynamics.beforeTick(ctx);
      assertFalse(network.isNeighbor(new Node.Id(1), new Node.Id(2)));

      // End algorithm
      dynamics.onAlgorithmEnd(ctx);

      // Link should be restored for next algorithm
      assertTrue(network.isNeighbor(new Node.Id(1), new Node.Id(2)));
    }

    @Test
    @DisplayName("should reset state for next algorithm run")
    void resetsStateForNextRun() {
      NetworkDynamicsConfig.ScheduledLinkFailures config =
          new NetworkDynamicsConfig.ScheduledLinkFailures(
              5, 0, List.of(new NetworkDynamicsConfig.ScheduledLinkFailures.LinkSpec(1, 2)));

      ScheduledLinkFailuresDynamics dynamics = new ScheduledLinkFailuresDynamics(config);

      // First algorithm run: disconnect
      for (int i = 0; i < 5; i++) {
        ctx.advanceOneTick();
      }
      dynamics.beforeTick(ctx);
      assertFalse(network.isNeighbor(new Node.Id(1), new Node.Id(2)));

      // End first algorithm
      dynamics.onAlgorithmEnd(ctx);

      // Reset context for second algorithm
      ctx.reset(AlgorithmType.SHORTEST_PATH);

      // Link should be connected
      assertTrue(network.isNeighbor(new Node.Id(1), new Node.Id(2)));

      // Second algorithm run: should disconnect again at tick 5
      for (int i = 0; i < 4; i++) {
        ctx.advanceOneTick();
        dynamics.beforeTick(ctx);
      }
      // Not yet at tick 5
      assertTrue(network.isNeighbor(new Node.Id(1), new Node.Id(2)));

      ctx.advanceOneTick();
      dynamics.beforeTick(ctx);
      // Now at tick 5
      assertFalse(network.isNeighbor(new Node.Id(1), new Node.Id(2)));
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCases {

    @Test
    @DisplayName("should handle disconnect at tick 0")
    void disconnectAtTickZero() {
      NetworkDynamicsConfig.ScheduledLinkFailures config =
          new NetworkDynamicsConfig.ScheduledLinkFailures(
              0, 0, List.of(new NetworkDynamicsConfig.ScheduledLinkFailures.LinkSpec(1, 2)));

      ScheduledLinkFailuresDynamics dynamics = new ScheduledLinkFailuresDynamics(config);

      dynamics.beforeTick(ctx);

      assertFalse(network.isNeighbor(new Node.Id(1), new Node.Id(2)));
    }

    @Test
    @DisplayName("should handle already disconnected link gracefully")
    void handlesAlreadyDisconnectedLink() {
      // Manually disconnect the link first
      Node node1 = network.getNode(new Node.Id(1));
      Node node2 = network.getNode(new Node.Id(2));
      node1.getNeighbors().remove(node2);
      node2.getNeighbors().remove(node1);

      NetworkDynamicsConfig.ScheduledLinkFailures config =
          new NetworkDynamicsConfig.ScheduledLinkFailures(
              0, 0, List.of(new NetworkDynamicsConfig.ScheduledLinkFailures.LinkSpec(1, 2)));

      ScheduledLinkFailuresDynamics dynamics = new ScheduledLinkFailuresDynamics(config);

      // Should not throw
      assertDoesNotThrow(() -> dynamics.beforeTick(ctx));
    }

    @Test
    @DisplayName("should not add duplicate links on reconnect")
    void noDuplicateLinksOnReconnect() {
      NetworkDynamicsConfig.ScheduledLinkFailures config =
          new NetworkDynamicsConfig.ScheduledLinkFailures(
              1, 3, List.of(new NetworkDynamicsConfig.ScheduledLinkFailures.LinkSpec(1, 2)));

      ScheduledLinkFailuresDynamics dynamics = new ScheduledLinkFailuresDynamics(config);

      // Disconnect
      ctx.advanceOneTick();
      dynamics.beforeTick(ctx);

      // Reconnect
      ctx.advanceOneTick();
      ctx.advanceOneTick();
      dynamics.beforeTick(ctx);

      Node node1 = network.getNode(new Node.Id(1));

      // Count how many times node2 appears in neighbors
      long count = node1.getNeighbors().stream().filter(n -> n.getId().value() == 2).count();

      assertEquals(1, count, "Node 2 should appear exactly once in neighbors");
    }
  }
}
