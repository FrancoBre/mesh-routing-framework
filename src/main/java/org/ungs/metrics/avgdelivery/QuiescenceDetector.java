package org.ungs.metrics.avgdelivery;

import java.util.ArrayDeque;
import java.util.Deque;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class QuiescenceDetector {

  // =======================
  // === CONFIG CONSTANTS ===
  // =======================

  public static final int MIN_WARMUP_TICKS = 0; // don't even try to detect before this
  public static final int QUIESCENCE_WINDOW_SAMPLES = 30; // W
  public static final int REQUIRED_STABLE_WINDOWS = 8; // S
  public static final double EPSILON_RANGE = 2.0; // threshold on (max - min)

  // ======================
  // === STATE VARIABLES ==
  // ======================

  public static boolean measuring = false;
  public static int stableWindowsInARow = 0;

  // index to measure only "newly received" packets
  public static int lastSeenReceivedIdx = 0;

  // keep the last W averages to detect stabilization
  public static final Deque<Double> recentSampleMeans = new ArrayDeque<>();

  // optional: final mean after quiescence (Figure 4 style)
  public static double postQuiescenceSum = 0.0;
  public static long postQuiescenceCount = 0;

  // ======================
  // === CORE LOGIC =======
  // ======================

  /**
   * Feed a new sampled average delivery time.
   *
   * @param simulationTime current simulation tick
   * @param sampledAvgDeliveryTime average delivery time computed for this sample
   * @return true if we are in measuring phase (after quiescence)
   */
  public static boolean update(double simulationTime, double sampledAvgDeliveryTime) {

    // already measuring → just accumulate stats
    if (measuring) {
      postQuiescenceSum += sampledAvgDeliveryTime;
      postQuiescenceCount++;
      return true;
    }

    // too early → ignore
    if (simulationTime < MIN_WARMUP_TICKS) {
      return false;
    }

    // feed sliding window
    recentSampleMeans.addLast(sampledAvgDeliveryTime);
    if (recentSampleMeans.size() > QUIESCENCE_WINDOW_SAMPLES) {
      recentSampleMeans.removeFirst();
    }

    // window not full yet
    if (recentSampleMeans.size() < QUIESCENCE_WINDOW_SAMPLES) {
      return false;
    }

    // compute range (max - min)
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;

    for (double v : recentSampleMeans) {
      if (v < min) min = v;
      if (v > max) max = v;
    }

    double range = max - min;

    if (range <= EPSILON_RANGE) {
      stableWindowsInARow++;
    } else {
      stableWindowsInARow = 0;
    }

    if (stableWindowsInARow >= REQUIRED_STABLE_WINDOWS) {
      measuring = true;
      return true;
    }

    return false;
  }

  // ======================
  // === HELPERS ==========
  // ======================

  public static boolean isMeasuring() {
    return measuring;
  }

  public static double meanAfterQuiescence() {
    if (postQuiescenceCount == 0) {
      return 0.0;
    }
    return postQuiescenceSum / postQuiescenceCount;
  }

  // ======================
  // === RESET ============
  // ======================

  public static void reset() {
    measuring = false;
    stableWindowsInARow = 0;
    lastSeenReceivedIdx = 0;

    recentSampleMeans.clear();

    postQuiescenceSum = 0.0;
    postQuiescenceCount = 0;
  }
}
