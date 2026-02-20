package org.ungs.cli;

import java.util.List;
import org.aeonbits.owner.Accessible;
import org.aeonbits.owner.Config;

@Config.Sources("classpath:application.properties")
public interface SimulationConfigLoader extends Config, Accessible {

  // -----------------------
  // GENERAL
  // -----------------------

  @Key("seed")
  @DefaultValue("42")
  long seed();

  @Key("topology")
  @DefaultValue("GRID_6X6")
  String topology();

  @Key("topology.file")
  @DefaultValue("")
  String topologyFile();

  @Key("algorithms")
  @DefaultValue("Q_ROUTING")
  @Separator(",")
  List<String> algorithms();

  @Key("max-active-packets")
  @DefaultValue("") // empty => unlimited
  String maxActivePackets();

  @Key("warmup-ticks")
  @DefaultValue("0")
  int warmupTicks();

  @Key("experiment-name")
  @DefaultValue("") // empty => auto
  String experimentName();

  @Key("output-folder")
  @DefaultValue("")
  String outputFolder();

  // -----------------------
  // TERMINATION
  // -----------------------

  @Key("termination-policy")
  @DefaultValue("FIXED_TICKS")
  String terminationPolicy();

  @Key("termination-policy.composite.mode")
  @DefaultValue("OR")
  String terminationCompositeMode();

  @Key("termination-policy.composite.policies")
  @DefaultValue("")
  @Separator(",")
  List<String> terminationCompositePolicies();

  @Key("termination-policy.fixed-ticks.total-ticks")
  @DefaultValue("15000")
  long terminationFixedTicksTotalTicks();

  @Key("termination-policy.packets-delivered.total-packets")
  @DefaultValue("")
  String terminationTotalPacketsDelivered(); // empty => not set

  // -----------------------
  // INJECTION SCHEDULE
  // -----------------------

  @Key("injection-schedule")
  @DefaultValue("LOAD_LEVEL")
  String injectionSchedule();

  // LOAD_LEVEL
  @Key("injection-schedule.load-level.L")
  @DefaultValue("0.0")
  double injectionLoadLevelL();

  // PROB_PER_TICK
  @Key("injection-schedule.prob-per-tick.p")
  @DefaultValue("0.0")
  double injectionProbPerTickP();

  // GAP
  @Key("injection-schedule.gap.inject-every-n-ticks")
  @DefaultValue("1")
  int injectionGapInjectEveryNTicks();

  @Key("injection-schedule.gap.batch-size")
  @DefaultValue("1")
  int injectionGapBatchSize();

  // WINDOWED_LOAD (defaults from WindowedLoadConstants)
  @Key("injection-schedule.windowed-load.phase-a.ticks")
  @DefaultValue("200")
  int injectionWindowedPhaseATicks();

  @Key("injection-schedule.windowed-load.phase-a.batch")
  @DefaultValue("2")
  int injectionWindowedPhaseABatch();

  @Key("injection-schedule.windowed-load.phase-b.ticks")
  @DefaultValue("800")
  int injectionWindowedPhaseBTicks();

  @Key("injection-schedule.windowed-load.phase-b.batch")
  @DefaultValue("10")
  int injectionWindowedPhaseBBatch();

  @Key("injection-schedule.windowed-load.phase-c.batch")
  @DefaultValue("0")
  int injectionWindowedPhaseCBatch();

  // PLATEAU_THEN_LINEAR (defaults from PlateauThenLinearConstants)
  @Key("injection-schedule.plateau-then-linear.plateau.ticks")
  @DefaultValue("200")
  int injectionPlateauThenLinearPlateauTicks();

  @Key("injection-schedule.plateau-then-linear.plateau.inject-every-n-ticks")
  @DefaultValue("5")
  int injectionPlateauThenLinearPlateauInjectEveryNTicks();

  @Key("injection-schedule.plateau-then-linear.plateau.batch-size")
  @DefaultValue("2")
  int injectionPlateauThenLinearPlateauBatchSize();

  @Key("injection-schedule.plateau-then-linear.ramp.inject-every-n-ticks")
  @DefaultValue("1")
  int injectionPlateauThenLinearRampInjectEveryNTicks();

  @Key("injection-schedule.plateau-then-linear.ramp.start-batch-size")
  @DefaultValue("2")
  int injectionPlateauThenLinearRampStartBatchSize();

  @Key("injection-schedule.plateau-then-linear.ramp.increase-every-n-ticks")
  @DefaultValue("200")
  int injectionPlateauThenLinearRampIncreaseEveryNTicks();

  // PLATEAU_RAMP_PLATEAU (defaults from PlateauRampPlateauConstants)
  @Key("injection-schedule.plateau-ramp-plateau.p1.ticks")
  @DefaultValue("200")
  int injectionPlateauRampPlateauP1Ticks();

  @Key("injection-schedule.plateau-ramp-plateau.p1.inject-every-n-ticks")
  @DefaultValue("5")
  int injectionPlateauRampPlateauP1InjectEveryNTicks();

  @Key("injection-schedule.plateau-ramp-plateau.p1.batch-size")
  @DefaultValue("2")
  int injectionPlateauRampPlateauP1BatchSize();

  @Key("injection-schedule.plateau-ramp-plateau.ramp.inject-every-n-ticks")
  @DefaultValue("1")
  int injectionPlateauRampPlateauRampInjectEveryNTicks();

  @Key("injection-schedule.plateau-ramp-plateau.ramp.start-batch-size")
  @DefaultValue("2")
  int injectionPlateauRampPlateauRampStartBatchSize();

  @Key("injection-schedule.plateau-ramp-plateau.ramp.increase-every-n-ticks")
  @DefaultValue("200")
  int injectionPlateauRampPlateauRampIncreaseEveryNTicks();

  @Key("injection-schedule.plateau-ramp-plateau.ramp.ticks")
  @DefaultValue("600")
  int injectionPlateauRampPlateauRampTicks();

  @Key("injection-schedule.plateau-ramp-plateau.ramp.max-batch-size")
  @DefaultValue("10")
  int injectionPlateauRampPlateauRampMaxBatchSize();

  @Key("injection-schedule.plateau-ramp-plateau.p3.inject-every-n-ticks")
  @DefaultValue("1")
  int injectionPlateauRampPlateauP3InjectEveryNTicks();

  @Key("injection-schedule.plateau-ramp-plateau.p3.batch-size")
  @DefaultValue("10")
  int injectionPlateauRampPlateauP3BatchSize();

  // FIXED_LOAD_STEP (defaults from FixedLoadStepConstants)
  @Key("injection-schedule.fixed-load-step.step-ticks")
  @DefaultValue("200")
  int injectionFixedLoadStepStepTicks();

  @Key("injection-schedule.fixed-load-step.inject-every-n-ticks")
  @DefaultValue("1")
  int injectionFixedLoadStepInjectEveryNTicks();

  @Key("injection-schedule.fixed-load-step.batch-sizes")
  @DefaultValue("1,2,3,4,5,6,7")
  @Separator(",")
  List<String> injectionFixedLoadStepBatchSizes();

  @Key("injection-schedule.minL")
  @DefaultValue("0.0")
  double injectionMinL();

  @Key("injection-schedule.maxL")
  @DefaultValue("3.5")
  double injectionMaxL();

  @Key("injection-schedule.load-level-change.period-ticks")
  @DefaultValue("20000")
  int injectionLoadLevelChangePeriodTicks();

  @Key("injection-schedule.segmentwise.segments")
  @DefaultValue("")
  String injectionSegmentwiseSegments();

  // -----------------------
  // PAIR SELECTION
  // -----------------------

  @Key("pair-selection")
  @DefaultValue("RANDOM")
  String pairSelection();

  @Key("pair-selection.oscillating.period-ticks")
  @DefaultValue("200")
  int pairSelectionOscillatingPeriodTicks();

  @Key("pair-selection.constraints.disallow-self")
  @DefaultValue("true")
  boolean pairSelectionDisallowSelf();

  @Key("pair-selection.constraints.disallow-neighbor")
  @DefaultValue("true")
  boolean pairSelectionDisallowNeighbor();

  // group-based selectors
  @Key("pair-selection.groups.from")
  @DefaultValue("")
  String pairSelectionFromGroup();

  @Key("pair-selection.groups.to")
  @DefaultValue("")
  String pairSelectionToGroup();

  @Key("pair-selection.oscillating.groups.a")
  @DefaultValue("")
  String pairSelectionOscillatingGroupA();

  @Key("pair-selection.oscillating.groups.b")
  @DefaultValue("")
  String pairSelectionOscillatingGroupB();

  // -----------------------
  // GROUPS
  // -----------------------

  @Key("groups")
  @DefaultValue("")
  @Separator(",")
  List<String> groups();

  // NOTE: group nodes are dynamic keys: groups.<NAME>.nodes=...
  // OWNER doesn't support strongly typed dynamic methods cleanly;
  // we'll fetch them via raw Config in the mapping layer.

  // -----------------------
  // NETWORK DYNAMICS
  // -----------------------

  @Key("network-dynamics")
  @DefaultValue("NONE")
  String networkDynamics();

  @Key("network-dynamics.node-failures.model")
  @DefaultValue("RANDOM")
  String nodeFailuresModel();

  @Key("network-dynamics.node-failures.random.p")
  @DefaultValue("0.001")
  double nodeFailuresRandomP();

  @Key("network-dynamics.node-failures.random.mean-downtime-ticks")
  @DefaultValue("500")
  int nodeFailuresMeanDowntimeTicks();

  @Key("network-dynamics.node-failures.random.mean-uptime-ticks")
  @DefaultValue("2000")
  int nodeFailuresMeanUptimeTicks();

  // -----------------------
  // METRICS / OUTPUTS
  // -----------------------

  @Key("metrics")
  @DefaultValue("AVG_DELIVERY_TIME")
  @Separator(",")
  List<String> metrics();

  @Key("outputs")
  @DefaultValue("")
  @Separator(",")
  List<String> outputs();

  @Key("output.sample-every-ticks")
  @DefaultValue("1")
  int outputSampleEveryTicks();

  @Key("output.heatmap.from-tick")
  @DefaultValue("0")
  long heatmapFromTick();

  @Key("output.heatmap.to-tick")
  @DefaultValue("")
  String heatmapToTick();
}
