package org.ungs.core.traffic.schedule;

import org.ungs.core.config.InjectionScheduleConfig;
import org.ungs.core.observability.events.LoadLevelUpdatedEvent;

public final class SegmentwiseLoadLevelSchedulePreset implements InjectionSchedulePreset {

  @Override
  public InjectionScheduleType type() {
    return InjectionScheduleType.SEGMENTWISE_LOAD_LEVEL;
  }

  @Override
  public InjectionSchedule create(InjectionScheduleConfig cfg) {
    var c = (InjectionScheduleConfig.SegmentwiseLoadLevel) cfg;
    var segments = c.segments();

    if (segments.isEmpty()) {
      throw new IllegalArgumentException("piecewise-load-level.segments must not be empty");
    }

    // Precompute segment boundaries for fast lookup by tick
    long[] start = new long[segments.size()];
    long[] end = new long[segments.size()];

    long acc = 0;
    for (int i = 0; i < segments.size(); i++) {
      var s = segments.get(i);

      if (s.ticks() <= 0) {
        throw new IllegalArgumentException("piecewise-load-level.segment.ticks must be > 0");
      }

      start[i] = acc;
      acc += s.ticks();
      end[i] = acc; // exclusive
    }

    final long total = acc;

    return ctx -> {
      long t = (long) ctx.getTick();

      // If simulation goes beyond total, clamp at last segment end.
      if (t < 0) t = 0;
      if (t >= total) t = total - 1;

      int i = findSegmentIndex(t, start, end);
      var seg = segments.get(i);

      long tickInsideSegment = t - start[i];

      double L;
      LoadLevelUpdatedEvent.LoadLevelTrend trend;

      if (seg instanceof InjectionScheduleConfig.SegmentwiseLoadLevel.Plateau p) {
        L = p.L();
        trend = LoadLevelUpdatedEvent.LoadLevelTrend.PLATEAU;

      } else {
        var r = (InjectionScheduleConfig.SegmentwiseLoadLevel.Ramp) seg;

        double phase = (double) tickInsideSegment / (double) Math.max(1, r.ticks());

        if (phase < 0.0) phase = 0.0;
        if (phase > 1.0) phase = 1.0;

        L = r.fromL() + phase * (r.toL() - r.fromL());

        if (r.toL() > r.fromL()) {
          trend = LoadLevelUpdatedEvent.LoadLevelTrend.RISING;
        } else if (r.toL() < r.fromL()) {
          trend = LoadLevelUpdatedEvent.LoadLevelTrend.FALLING;
        } else {
          trend = LoadLevelUpdatedEvent.LoadLevelTrend.PLATEAU;
        }
      }

      int base = (int) Math.floor(L);
      double frac = L - base;

      int inject = base;
      if (frac > 0.0) {
        if (ctx.getRng().nextUnitDouble() < frac) inject += 1;
      }

      ctx.getEventSink().emit(new LoadLevelUpdatedEvent(ctx.getTick(), L, trend));

      return inject;
    };
  }

  private static int findSegmentIndex(long t, long[] start, long[] end) {
    for (int i = 0; i < start.length; i++) {
      if (t >= start[i] && t < end[i]) return i;
    }
    return start.length - 1;
  }
}
