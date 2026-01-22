package org.ungs.util;

public final class DeterministicRng {

  private long state;

  public DeterministicRng(long seed) {
    this.state = seed;
  }

  // SplitMix64
  public long nextLong() {
    long z = (state += 0x9E3779B97F4A7C15L);
    z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
    z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
    return z ^ (z >>> 31);
  }

  public int nextInt(int _int) {
    return (int) nextLong() % _int;
  }

  public int nextIndex(int bound) {
    if (bound <= 0) {
      throw new IllegalArgumentException("bound must be > 0");
    }
    long r = nextLong();
    return (int) Long.remainderUnsigned(r, bound);
  }

  public double nextUnitDouble() {
    // 53 bits of precision like java Random.nextDouble()
    long r = nextLong();
    long x = (r >>> 11); // keep top 53 bits
    return x * (1.0 / (1L << 53));
  }
}
