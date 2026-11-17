package dev.ccgcm.platform.orcanexus.mockdb.seed;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Outcome of a seed run.
 *
 * @param exitCode 0 on success, 1 on any failure
 * @param rowCounts rows inserted per table, in insertion order; empty on failure
 * @param notes warnings and skips found while seeding; empty on failure
 */
public record SeedResult(int exitCode, Map<String, Integer> rowCounts, List<SeedNote> notes) {

  public SeedResult {
    rowCounts = copyOrdered(rowCounts);
    notes = List.copyOf(notes);
  }

  /** A copy, in insertion order. */
  @Override
  public Map<String, Integer> rowCounts() {
    return new LinkedHashMap<>(rowCounts);
  }

  /** A failed run that inserted nothing. */
  public static SeedResult failure() {
    return new SeedResult(1, Map.of(), List.of());
  }

  /** Total rows over all tables. */
  public long totalRows() {
    long total = 0;
    for (int n : rowCounts.values()) {
      total += n;
    }
    return total;
  }

  private static Map<String, Integer> copyOrdered(Map<String, Integer> source) {
    return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(source));
  }
}
