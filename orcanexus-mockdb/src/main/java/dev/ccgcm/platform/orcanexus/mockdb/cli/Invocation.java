package dev.ccgcm.platform.orcanexus.mockdb.cli;

import java.util.Objects;

/** A validated command line. */
public record Invocation(Command command, int scale, int seed) {

  public static final int DEFAULT_SCALE = 100;
  public static final int DEFAULT_SEED = 42;
  public static final int MIN_SCALE = 1;
  public static final int MAX_SCALE = 100_000;

  public Invocation {
    Objects.requireNonNull(command, "command");
  }

  /** An invocation with the default scale and seed. */
  public static Invocation of(Command command) {
    return new Invocation(command, DEFAULT_SCALE, DEFAULT_SEED);
  }
}
