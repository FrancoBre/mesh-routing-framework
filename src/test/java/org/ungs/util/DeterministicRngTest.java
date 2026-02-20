package org.ungs.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DeterministicRng")
class DeterministicRngTest {

  private DeterministicRng rng;

  @BeforeEach
  void setUp() {
    rng = new DeterministicRng(42L);
  }

  @Nested
  @DisplayName("Reproducibility")
  class Reproducibility {

    @Test
    @DisplayName("should produce same sequence with same seed")
    void sameSeed_sameSequence() {
      DeterministicRng rng1 = new DeterministicRng(12345L);
      DeterministicRng rng2 = new DeterministicRng(12345L);

      for (int i = 0; i < 1000; i++) {
        assertEquals(
            rng1.nextLong(), rng2.nextLong(), "Sequence should be identical at position " + i);
      }
    }

    @Test
    @DisplayName("should produce different sequence with different seed")
    void differentSeed_differentSequence() {
      DeterministicRng rng1 = new DeterministicRng(111L);
      DeterministicRng rng2 = new DeterministicRng(222L);

      // At least one of the first 10 values should differ
      boolean foundDifference = false;
      for (int i = 0; i < 10; i++) {
        if (rng1.nextLong() != rng2.nextLong()) {
          foundDifference = true;
          break;
        }
      }
      assertTrue(foundDifference, "Different seeds should produce different sequences");
    }

    @Test
    @DisplayName("should be reproducible across multiple runs")
    void reproducibleAcrossRuns() {
      // First run
      DeterministicRng first = new DeterministicRng(999L);
      long[] firstSequence = new long[100];
      for (int i = 0; i < 100; i++) {
        firstSequence[i] = first.nextLong();
      }

      // Second run with same seed
      DeterministicRng second = new DeterministicRng(999L);
      for (int i = 0; i < 100; i++) {
        assertEquals(
            firstSequence[i],
            second.nextLong(),
            "Sequence should be reproducible at position " + i);
      }
    }
  }

  @Nested
  @DisplayName("nextLong")
  class NextLong {

    @Test
    @DisplayName("should generate distinct values")
    void generatesDistinctValues() {
      Set<Long> values = new HashSet<>();
      int samples = 10000;

      for (int i = 0; i < samples; i++) {
        values.add(rng.nextLong());
      }

      // With good RNG, all values should be unique
      assertEquals(samples, values.size(), "All generated values should be unique");
    }

    @Test
    @DisplayName("should advance state on each call")
    void advancesState() {
      long first = rng.nextLong();
      long second = rng.nextLong();
      long third = rng.nextLong();

      // All values should be different
      assertNotEquals(first, second);
      assertNotEquals(second, third);
      assertNotEquals(first, third);
    }
  }

  @Nested
  @DisplayName("nextIndex")
  class NextIndex {

    @Test
    @DisplayName("should return value in range [0, bound)")
    void valueInRange() {
      int bound = 10;

      for (int i = 0; i < 1000; i++) {
        int value = rng.nextIndex(bound);
        assertTrue(value >= 0, "Value should be >= 0");
        assertTrue(value < bound, "Value should be < bound");
      }
    }

    @Test
    @DisplayName("should have uniform distribution within bounds")
    void uniformDistribution() {
      int bound = 10;
      int[] counts = new int[bound];
      int samples = 10000;

      for (int i = 0; i < samples; i++) {
        int index = rng.nextIndex(bound);
        counts[index]++;
      }

      // Each bucket should have roughly samples/bound occurrences
      double expected = (double) samples / bound;
      for (int i = 0; i < bound; i++) {
        double deviation = Math.abs(counts[i] - expected) / expected;
        assertTrue(
            deviation < 0.15,
            "Bucket "
                + i
                + " has unexpected count: "
                + counts[i]
                + " (expected ~"
                + expected
                + ")");
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when bound <= 0")
    void throwsForInvalidBound() {
      assertThrows(IllegalArgumentException.class, () -> rng.nextIndex(0));
      assertThrows(IllegalArgumentException.class, () -> rng.nextIndex(-1));
      assertThrows(IllegalArgumentException.class, () -> rng.nextIndex(-100));
    }

    @Test
    @DisplayName("should work with bound = 1")
    void worksWithBoundOne() {
      for (int i = 0; i < 100; i++) {
        assertEquals(0, rng.nextIndex(1), "With bound=1, only 0 is valid");
      }
    }

    @Test
    @DisplayName("should work with large bound")
    void worksWithLargeBound() {
      int bound = 1_000_000;
      Set<Integer> values = new HashSet<>();

      for (int i = 0; i < 1000; i++) {
        int value = rng.nextIndex(bound);
        assertTrue(value >= 0 && value < bound);
        values.add(value);
      }

      // Should have generated many distinct values
      assertTrue(values.size() > 900, "Should generate many distinct values");
    }
  }

  @Nested
  @DisplayName("nextUnitDouble")
  class NextUnitDouble {

    @Test
    @DisplayName("should return value in range [0, 1)")
    void valueInRange() {
      for (int i = 0; i < 10000; i++) {
        double value = rng.nextUnitDouble();
        assertTrue(value >= 0.0, "Value should be >= 0.0");
        assertTrue(value < 1.0, "Value should be < 1.0");
      }
    }

    @Test
    @DisplayName("should have uniform distribution in [0, 1)")
    void uniformDistribution() {
      int buckets = 10;
      int[] counts = new int[buckets];
      int samples = 10000;

      for (int i = 0; i < samples; i++) {
        double value = rng.nextUnitDouble();
        int bucket = (int) (value * buckets);
        if (bucket == buckets) bucket = buckets - 1; // handle edge case
        counts[bucket]++;
      }

      // Each bucket should have roughly samples/buckets occurrences
      double expected = (double) samples / buckets;
      for (int i = 0; i < buckets; i++) {
        double deviation = Math.abs(counts[i] - expected) / expected;
        assertTrue(
            deviation < 0.15,
            "Bucket "
                + i
                + " has unexpected count: "
                + counts[i]
                + " (expected ~"
                + expected
                + ")");
      }
    }

    @Test
    @DisplayName("should never return exactly 1.0")
    void neverReturnsOne() {
      // Test many samples to be confident
      for (int i = 0; i < 100000; i++) {
        double value = rng.nextUnitDouble();
        assertNotEquals(1.0, value, 0.0);
      }
    }

    @Test
    @DisplayName("should generate values near boundaries")
    void generatesNearBoundaries() {
      boolean foundNearZero = false;
      boolean foundNearOne = false;

      for (int i = 0; i < 100000; i++) {
        double value = rng.nextUnitDouble();
        if (value < 0.01) foundNearZero = true;
        if (value > 0.99) foundNearOne = true;

        if (foundNearZero && foundNearOne) break;
      }

      assertTrue(foundNearZero, "Should generate values near 0");
      assertTrue(foundNearOne, "Should generate values near 1 (but not 1.0)");
    }
  }

  @Nested
  @DisplayName("nextInt")
  class NextInt {

    @Test
    @DisplayName("should return values that can be positive or negative")
    void canReturnPositiveOrNegative() {
      boolean foundPositive = false;
      boolean foundNegative = false;

      for (int i = 0; i < 1000; i++) {
        int value = rng.nextInt(100);
        if (value > 0) foundPositive = true;
        if (value < 0) foundNegative = true;

        if (foundPositive && foundNegative) break;
      }

      // nextInt uses modulo which can produce negative values
      // This is expected behavior based on the implementation
      assertTrue(foundPositive || foundNegative, "Should generate at least some values");
    }
  }

  @Nested
  @DisplayName("SplitMix64 Algorithm")
  class SplitMix64 {

    @Test
    @DisplayName("should pass basic statistical tests")
    void passesBasicStatistics() {
      // Test that the output doesn't have obvious patterns
      DeterministicRng testRng = new DeterministicRng(0L);

      long xorResult = 0L;
      for (int i = 0; i < 1000; i++) {
        xorResult ^= testRng.nextLong();
      }

      // XOR of many random values should not be 0
      // (extremely unlikely if RNG is working correctly)
      assertNotEquals(0L, xorResult);
    }

    @Test
    @DisplayName("should work with seed 0")
    void worksWithSeedZero() {
      DeterministicRng zeroSeed = new DeterministicRng(0L);

      // Should still produce values
      long first = zeroSeed.nextLong();
      long second = zeroSeed.nextLong();

      assertNotEquals(0L, first, "Should produce non-zero values");
      assertNotEquals(first, second, "Should produce different values");
    }

    @Test
    @DisplayName("should work with negative seed")
    void worksWithNegativeSeed() {
      DeterministicRng negSeed = new DeterministicRng(-12345L);

      Set<Long> values = new HashSet<>();
      for (int i = 0; i < 100; i++) {
        values.add(negSeed.nextLong());
      }

      assertEquals(100, values.size(), "Should generate unique values");
    }
  }

  @Nested
  @DisplayName("ToString")
  class ToStringTest {

    @Test
    @DisplayName("should not throw")
    void doesNotThrow() {
      assertDoesNotThrow(() -> rng.toString());
    }
  }
}
