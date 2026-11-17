package dev.ccgcm.platform.orcanexus.mockdb.seed;

import java.util.Objects;

/**
 * A warning or skip message produced while seeding.
 *
 * @param kind what happened
 * @param table the table concerned
 * @param column the column concerned, or {@code null} when the note applies to the whole table
 * @param message human-readable text
 */
public record SeedNote(Kind kind, String table, String column, String message) {

  /** Note categories. */
  public enum Kind {
    /** The table has no relationship spec entry; type-valid values were generated. */
    NO_SPEC,
    /** A boundary row was skipped for a column. */
    BOUNDARY_SKIPPED
  }

  public SeedNote {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(table, "table");
    Objects.requireNonNull(message, "message");
  }
}
