package dev.ccgcm.platform.orcanexus.mockdb.seed;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Immutable pools of fictional identifiers. Ids are computed from the pool index, so they are
 * unique by construction and nothing is stored per id.
 */
public final class KeyPools {

  private static final String[] BUCKETS = {
    "mock-bucket-ingest", "mock-bucket-results", "mock-bucket-archive"
  };
  private static final long HEX_MULTIPLIER = 2654435761L;

  private final int libraries;
  private final int[] runEpochDays;
  private final int[] workflowEpochDays;

  /**
   * Builds the pools.
   *
   * @param libraries library count (the Scale)
   * @param runDates date of each sequencing run (its size is the run pool size)
   * @param workflowDates date of each workflow (its size is the portal run pool size)
   */
  KeyPools(int libraries, LocalDate[] runDates, LocalDate[] workflowDates) {
    this.libraries = libraries;
    this.runEpochDays = toEpochDays(runDates);
    this.workflowEpochDays = toEpochDays(workflowDates);
  }

  private static int[] toEpochDays(LocalDate[] dates) {
    var days = new int[dates.length];
    for (int i = 0; i < dates.length; i++) {
      days[i] = Math.toIntExact(dates[i].toEpochDay());
    }
    return days;
  }

  /** Number of runs for a scale. */
  static int runCount(int libraries) {
    return libraries == 1 ? 1 : Math.max(2, ceilDiv(libraries, 10));
  }

  static int subjectCount(int libraries) {
    return ceilDiv(libraries, 2);
  }

  static int cohortCount(int libraries) {
    return Math.max(1, ceilDiv(libraries, 20));
  }

  private static int ceilDiv(int a, int b) {
    return (a + b - 1) / b;
  }

  /** Number of ids in the pool. */
  public int size(Pool pool) {
    return switch (pool) {
      case SUBJECT -> subjectCount(libraries);
      case SAMPLE, LIBRARY -> libraries;
      case SEQUENCING_RUN -> runEpochDays.length;
      case PORTAL_RUN -> workflowEpochDays.length;
      case COHORT -> cohortCount(libraries);
      case BUCKET -> BUCKETS.length;
    };
  }

  /** Id at the given pool index. */
  public String id(Pool pool, int index) {
    if (index < 0 || index >= size(pool)) {
      throw new IndexOutOfBoundsException(pool + " index " + index + " of " + size(pool));
    }
    return switch (pool) {
      case SUBJECT -> String.format(Locale.ROOT, "SBJ%05d", index);
      case SAMPLE -> String.format(Locale.ROOT, "PRJ%05d", index);
      case LIBRARY -> libraryId(index);
      case SEQUENCING_RUN -> {
        var d = LocalDate.ofEpochDay(runEpochDays[index]);
        yield String.format(
            Locale.ROOT,
            "%02d%02d%02d_MOCK%02d_%04d_AMOCK%02d",
            d.getYear() % 100,
            d.getMonthValue(),
            d.getDayOfMonth(),
            index % 100,
            index % 10000,
            index % 100);
      }
      case PORTAL_RUN -> {
        var d = LocalDate.ofEpochDay(workflowEpochDays[index]);
        long hex = ((long) index * HEX_MULTIPLIER) & 0xFFFFFFFFL;
        yield String.format(
            Locale.ROOT,
            "%04d%02d%02d%08x",
            d.getYear(),
            d.getMonthValue(),
            d.getDayOfMonth(),
            hex);
      }
      case COHORT -> String.format(Locale.ROOT, "COH-MOCK-%02d", index);
      case BUCKET -> BUCKETS[index];
    };
  }

  /** Library id {@code L} + 2-digit year (22 + i % 4) + 5-digit sequence (i / 4 + 1). */
  static String libraryId(int index) {
    return String.format(Locale.ROOT, "L%02d%05d", 22 + index % 4, index / 4 + 1);
  }

  /** All ids of a pool, in index order. */
  public List<String> ids(Pool pool) {
    int n = size(pool);
    var list = new ArrayList<String>(n);
    for (int i = 0; i < n; i++) {
      list.add(id(pool, i));
    }
    return List.copyOf(list);
  }
}
