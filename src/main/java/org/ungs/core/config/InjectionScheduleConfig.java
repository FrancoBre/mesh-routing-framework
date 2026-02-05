package org.ungs.core.config;

import java.util.List;
import org.ungs.cli.SimulationConfigLoader;
import org.ungs.core.traffic.schedule.InjectionScheduleType;

public interface InjectionScheduleConfig {

  InjectionScheduleType type();

  record LoadLevel(double L) implements InjectionScheduleConfig {
    @Override
    public InjectionScheduleType type() {
      return InjectionScheduleType.LOAD_LEVEL;
    }
  }

  record TriangularLoadLevel(double minL, double maxL, long periodTicks)
      implements InjectionScheduleConfig {
    @Override
    public InjectionScheduleType type() {
      return InjectionScheduleType.TRIANGULAR_LOAD_LEVEL;
    }
  }

  record LinearLoadLevel(double minL, double maxL, long periodTicks)
      implements InjectionScheduleConfig {
    @Override
    public InjectionScheduleType type() {
      return InjectionScheduleType.LINEAR_LOAD_LEVEL;
    }
  }

  record ProbPerTick(double p) implements InjectionScheduleConfig {
    @Override
    public InjectionScheduleType type() {
      return InjectionScheduleType.PROB_PER_TICK;
    }
  }

  record Gap(int injectEveryNTicks, int batchSize) implements InjectionScheduleConfig {
    @Override
    public InjectionScheduleType type() {
      return InjectionScheduleType.GAP;
    }
  }

  record WindowedLoad(
      int phaseATicks, int phaseABatch, int phaseBTicks, int phaseBBatch, int phaseCBatch)
      implements InjectionScheduleConfig {
    @Override
    public InjectionScheduleType type() {
      return InjectionScheduleType.WINDOWED_LOAD;
    }
  }

  record PlateauThenLinear(
      int plateauTicks,
      int plateauInjectEveryNTicks,
      int plateauBatchSize,
      int rampInjectEveryNTicks,
      int rampStartBatchSize,
      int rampIncreaseEveryNTicks)
      implements InjectionScheduleConfig {
    @Override
    public InjectionScheduleType type() {
      return InjectionScheduleType.PLATEAU_THEN_LINEAR;
    }
  }

  record PlateauRampPlateau(
      int p1Ticks,
      int p1InjectEveryNTicks,
      int p1BatchSize,
      int rampInjectEveryNTicks,
      int rampStartBatchSize,
      int rampIncreaseEveryNTicks,
      int rampTicks,
      int rampMaxBatchSize,
      int p3InjectEveryNTicks,
      int p3BatchSize)
      implements InjectionScheduleConfig {
    @Override
    public InjectionScheduleType type() {
      return InjectionScheduleType.PLATEAU_RAMP_PLATEAU;
    }
  }

  record FixedLoadStep(int stepTicks, int injectEveryNTicks, List<Integer> batchSizes)
      implements InjectionScheduleConfig {
    @Override
    public InjectionScheduleType type() {
      return InjectionScheduleType.FIXED_LOAD_STEP;
    }
  }

  static InjectionScheduleConfig fromLoader(SimulationConfigLoader l) {
    InjectionScheduleType type =
        SimulationConfigContext.parseEnum(l.injectionSchedule(), InjectionScheduleType.class);

    return switch (type) {
      case LOAD_LEVEL -> {
        double L = l.injectionLoadLevelL();
        if (L < 0.0)
          throw new IllegalArgumentException("traffic-schedule.load-level.L must be >= 0");
        yield new LoadLevel(L);
      }
      case PROB_PER_TICK -> {
        double p = l.injectionProbPerTickP();
        if (p < 0.0 || p > 1.0)
          throw new IllegalArgumentException("traffic-schedule.prob-per-tick.p must be in [0,1]");
        yield new ProbPerTick(p);
      }
      case GAP -> {
        int every = l.injectionGapInjectEveryNTicks();
        int batch = l.injectionGapBatchSize();
        if (every <= 0)
          throw new IllegalArgumentException(
              "traffic-schedule.gap.inject-every-n-ticks must be > 0");
        if (batch < 0)
          throw new IllegalArgumentException("traffic-schedule.gap.batch-size must be >= 0");
        yield new Gap(every, batch);
      }
      case WINDOWED_LOAD ->
          new WindowedLoad(
              l.injectionWindowedPhaseATicks(),
              l.injectionWindowedPhaseABatch(),
              l.injectionWindowedPhaseBTicks(),
              l.injectionWindowedPhaseBBatch(),
              l.injectionWindowedPhaseCBatch());
      case PLATEAU_THEN_LINEAR ->
          new PlateauThenLinear(
              l.injectionPlateauThenLinearPlateauTicks(),
              l.injectionPlateauThenLinearPlateauInjectEveryNTicks(),
              l.injectionPlateauThenLinearPlateauBatchSize(),
              l.injectionPlateauThenLinearRampInjectEveryNTicks(),
              l.injectionPlateauThenLinearRampStartBatchSize(),
              l.injectionPlateauThenLinearRampIncreaseEveryNTicks());
      case PLATEAU_RAMP_PLATEAU ->
          new PlateauRampPlateau(
              l.injectionPlateauRampPlateauP1Ticks(),
              l.injectionPlateauRampPlateauP1InjectEveryNTicks(),
              l.injectionPlateauRampPlateauP1BatchSize(),
              l.injectionPlateauRampPlateauRampInjectEveryNTicks(),
              l.injectionPlateauRampPlateauRampStartBatchSize(),
              l.injectionPlateauRampPlateauRampIncreaseEveryNTicks(),
              l.injectionPlateauRampPlateauRampTicks(),
              l.injectionPlateauRampPlateauRampMaxBatchSize(),
              l.injectionPlateauRampPlateauP3InjectEveryNTicks(),
              l.injectionPlateauRampPlateauP3BatchSize());
      case FIXED_LOAD_STEP -> {
        List<Integer> batchSizes =
            l.injectionFixedLoadStepBatchSizes().stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList();
        if (batchSizes.isEmpty())
          throw new IllegalArgumentException(
              "traffic-schedule.fixed-load-step.batch-sizes must not be empty");
        yield new FixedLoadStep(
            l.injectionFixedLoadStepStepTicks(),
            l.injectionFixedLoadStepInjectEveryNTicks(),
            batchSizes);
      }
      case TRIANGULAR_LOAD_LEVEL -> {
        double minL = l.injectionMinL();
        double maxL = l.injectionMaxL();
        long periodTicks = l.injectionLoadLevelChangePeriodTicks();

        if (minL < 0.0)
          throw new IllegalArgumentException("traffic-schedule.triangular.minL must be >= 0");
        if (maxL < 0.0)
          throw new IllegalArgumentException("traffic-schedule.triangular.maxL must be >= 0");
        if (maxL < minL)
          throw new IllegalArgumentException("traffic-schedule.triangular.maxL must be >= minL");
        if (periodTicks <= 0) periodTicks = l.terminationFixedTicksTotalTicks();

        yield new TriangularLoadLevel(minL, maxL, periodTicks);
      }
      case LINEAR_LOAD_LEVEL -> {
        double minL = l.injectionMinL();
        double maxL = l.injectionMaxL();
        long periodTicks = l.injectionLoadLevelChangePeriodTicks();

        if (minL < 0.0) throw new IllegalArgumentException("injection-schedule.minL must be >= 0");
        if (maxL < 0.0) throw new IllegalArgumentException("injection-schedule.maxL must be >= 0");
        if (maxL < minL)
          throw new IllegalArgumentException("injection-schedule.maxL must be >= minL");
        if (periodTicks <= 0) periodTicks = l.terminationFixedTicksTotalTicks();

        yield new LinearLoadLevel(minL, maxL, periodTicks);
      }
    };
  }
}
