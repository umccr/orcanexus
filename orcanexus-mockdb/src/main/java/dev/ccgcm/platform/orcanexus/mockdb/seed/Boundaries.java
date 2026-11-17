package dev.ccgcm.platform.orcanexus.mockdb.seed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import org.jooq.Field;

/** Boundary values and row placement for the edge-value rows. Pure; no clock. */
public final class Boundaries {

  /** Minimum rows for two boundary rows plus the null row. */
  public static final int MIN_ROWS_WITH_NULL = 3;

  /** Minimum rows for two boundary rows on a column that needs no null row. */
  public static final int MIN_ROWS_NO_NULL = 2;

  private Boundaries() {}

  /**
   * Where the boundary rows go.
   *
   * @param fits whether the boundary rows can be placed
   * @param minRow row index holding the minimum value (valid when {@code fits})
   * @param maxRow row index holding the maximum value (valid when {@code fits})
   */
  public record Placement(boolean fits, int minRow, int maxRow) {

    static final Placement SKIPPED = new Placement(false, -1, -1);
  }

  /**
   * Decides whether boundary rows fit. Needs {@code rows >= 3} when the column also needs a null
   * row, {@code rows >= 2} otherwise. When they fit, the minimum is on row 0 and the maximum on row
   * 1; the caller must protect both rows from the {@link NullPlan}.
   */
  public static Placement place(int rows, boolean needsNullRow) {
    int needed = needsNullRow ? MIN_ROWS_WITH_NULL : MIN_ROWS_NO_NULL;
    return rows >= needed ? new Placement(true, 0, 1) : Placement.SKIPPED;
  }

  // ---- NUMERIC -----------------------------------------------------------------------------

  /** True when the field declares a NUMERIC precision, so that a maximum exists. */
  public static boolean hasNumericBoundary(Field<?> field) {
    var type = field.getDataType();
    return type.hasPrecision()
        && type.precision() > 0
        && type.scale() >= 0
        && type.scale() <= type.precision();
  }

  /** Minimum of a NUMERIC column: zero at the declared scale; empty if no precision declared. */
  public static Optional<BigDecimal> numericMin(Field<?> field) {
    if (!hasNumericBoundary(field)) {
      return Optional.empty();
    }
    return Optional.of(BigDecimal.ZERO.setScale(field.getDataType().scale()));
  }

  /**
   * Maximum of a NUMERIC(p,s) column: {@code (10^(p-s) - 1) + (10^s - 1)/10^s} with scale s, for
   * example 99999999.9999 for (12,4).
   */
  public static Optional<BigDecimal> numericMax(Field<?> field) {
    if (!hasNumericBoundary(field)) {
      return Optional.empty();
    }
    return Optional.of(numericMax(field.getDataType().precision(), field.getDataType().scale()));
  }

  static BigDecimal numericMax(int precision, int scale) {
    BigDecimal integral = BigDecimal.TEN.pow(precision - scale).subtract(BigDecimal.ONE);
    BigDecimal fraction =
        BigDecimal.TEN
            .pow(scale)
            .subtract(BigDecimal.ONE)
            .divide(BigDecimal.TEN.pow(scale), scale, java.math.RoundingMode.UNNECESSARY);
    return integral.add(fraction).setScale(scale);
  }

  // ---- BIGINT ------------------------------------------------------------------------------

  public static long bigintMin() {
    return 0L;
  }

  public static long bigintMax() {
    return Long.MAX_VALUE;
  }

  // ---- DATE and TIMESTAMP ------------------------------------------------------------------

  public static LocalDate dateMin() {
    return SeedPlan.RANGE_START;
  }

  public static LocalDate dateMax() {
    return SeedPlan.RANGE_END;
  }

  /** Start of the first seeded day, 00:00:00. */
  public static LocalDateTime timestampMin() {
    return SeedPlan.RANGE_START.atStartOfDay();
  }

  /** End of the last seeded day at whole seconds, 23:59:59. */
  public static LocalDateTime timestampMax() {
    return SeedPlan.RANGE_END.atTime(LocalTime.of(23, 59, 59));
  }
}
