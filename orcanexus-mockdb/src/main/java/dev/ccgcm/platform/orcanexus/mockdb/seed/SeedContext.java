package dev.ccgcm.platform.orcanexus.mockdb.seed;

import java.util.Locale;
import java.util.Random;
import net.datafaker.Faker;

/**
 * The two seeding inputs (scale and seed value) plus named, independent random streams.
 *
 * <p>Each stream is seeded from a SplitMix64 mix of the seed value and the stream name, so adding
 * or reordering consumers never changes the data of the others.
 */
public record SeedContext(int scale, int seed) {

  private static final long GAMMA = 0x9E3779B97F4A7C15L;

  public SeedContext {
    if (scale < 1) {
      throw new IllegalArgumentException("scale must be >= 1, was " + scale);
    }
  }

  /** Returns a new {@link Random} for the named stream; same inputs give the same sequence. */
  public Random stream(String name) {
    return new Random(mix(seed, name));
  }

  /** Returns a Faker (English locale) whose choices follow the given random. */
  public Faker faker(Random random) {
    return new Faker(Locale.ENGLISH, random);
  }

  /** SplitMix64 over the seed and {@code name.hashCode()} (specified and stable for String). */
  static long mix(int seed, String name) {
    long z = ((long) seed) * GAMMA + name.hashCode();
    z += GAMMA;
    z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
    z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
    return z ^ (z >>> 31);
  }
}
