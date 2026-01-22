package org.ungs.cli;

import java.util.List;
import org.aeonbits.owner.Config;
import org.ungs.core.TopologyType;

@Config.Sources("classpath:application.properties")
public interface SimulationConfigLoader extends Config {

  @Key("topology")
  @DefaultValue("GRID_6X6")
  TopologyType topology();

  @Key("algorithms")
  @DefaultValue("Q_ROUTING")
  @Separator(",")
  List<String> algorithm();

  @Key("total-packets")
  @DefaultValue("0")
  int totalPackets();

  @Key("total-ticks")
  @DefaultValue("0")
  int totalTicks();

  @Key("packet-inject-gap")
  @DefaultValue("10")
  int packetInjectGap();

  @Key("seed")
  @DefaultValue("42")
  long seed();

  @Key("metrics")
  @DefaultValue("AVG_DELIVERY_TIME")
  @Separator(",")
  List<String> metrics();

  @Key("export-to")
  @DefaultValue("LOG_FILE")
  @Separator(",")
  List<String> exportTo();

  @Key("linear-incremental-packet-injection")
  @DefaultValue("false")
  boolean linearIncrementalPacketInjection();

  @Key("plateau-then-linear-packet-injection")
  @DefaultValue("false")
  boolean plateauThenLinearPacketInjection();

  @Key("plateau-ramp-plateau-packet-injection")
  @DefaultValue("false")
  boolean plateauRampPlateauPacketInjection();

  @Key("fixed-load-step-packet-injection")
  @DefaultValue("false")
  boolean fixedLoadStepPacketInjection();

  @Key("windowed-load-packet-injection")
  @DefaultValue("false")
  boolean windowedLoadPacketInjection();

  @Key("load-level")
  @DefaultValue("0.8")
  double loadLevel();
}
