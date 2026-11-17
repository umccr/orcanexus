package dev.ccgcm.platform.orcanexus.mockdb.seed;

import java.util.BitSet;
import java.util.Objects;
import java.util.Random;

/**
 * Null rows of one column. Pure: the outcome depends only on the row count, the protected rows and
 * the random stream passed in.
 *
 * <p>Null count {@code k}: 1 when {@code n < 10}; otherwise {@code clamp(round(f * n), ceil(0.10
 * n), floor(0.50 n))} with {@code f} drawn from [0.10, 0.30]. The null rows are picked by a partial
 * Fisher-Yates shuffle over the eligible (non-protected) rows. Pool columns and non-nullable
 * columns get {@link #none(int)}.
 */
public final class NullPlan {

  private static final int SMALL_TABLE = 10;
  private static final double F_MIN = 0.10;
  private static final double F_SPAN = 0.20;

  private final int rows;
  private final BitSet nulls;

  private NullPlan(int rows, BitSet nulls) {
    this.rows = rows;
    this.nulls = nulls;
  }

  /** A plan with no null rows, for pool columns and non-nullable columns. */
  public static NullPlan none(int rows) {
    checkRows(rows);
    return new NullPlan(rows, new BitSet(0));
  }

  /**
   * Plans the null rows of a nullable, non-pool column.
   *
   * @param rows number of rows {@code n} in the table
   * @param protectedRows rows that must not be null (boundary and tie rows); may be null
   * @param random the table's stream
   * @return the plan; empty (see {@link #isEmpty()}) when no row is eligible
   */
  public static NullPlan of(int rows, BitSet protectedRows, Random random) {
    checkRows(rows);
    Objects.requireNonNull(random, "random");
    // Draw f first so the stream advances identically whatever the protected rows are.
    double f = F_MIN + F_SPAN * random.nextDouble();
    int target = targetCount(rows, f);

    int[] eligible = new int[rows];
    int count = 0;
    for (int i = 0; i < rows; i++) {
      if (protectedRows == null || !protectedRows.get(i)) {
        eligible[count++] = i;
      }
    }
    int k = Math.min(target, count);
    var nulls = new BitSet(rows);
    for (int i = 0; i < k; i++) {
      int j = i + random.nextInt(count - i);
      int picked = eligible[j];
      eligible[j] = eligible[i];
      eligible[i] = picked;
      nulls.set(picked);
    }
    return new NullPlan(rows, nulls);
  }

  /** Target number of null rows for {@code rows} rows and fraction {@code f}. */
  static int targetCount(int rows, double f) {
    if (rows < SMALL_TABLE) {
      return 1;
    }
    int low = (int) Math.ceil(0.10 * rows);
    int high = (int) Math.floor(0.50 * rows);
    long rounded = Math.round(f * rows);
    return (int) Math.max(low, Math.min(high, rounded));
  }

  public boolean isNull(int row) {
    if (row < 0 || row >= rows) {
      throw new IndexOutOfBoundsException("row " + row + " of " + rows);
    }
    return nulls.get(row);
  }

  public int nullCount() {
    return nulls.cardinality();
  }

  public int rows() {
    return rows;
  }

  /** True when the plan has no null row (non-nullable or pool column, or no eligible row). */
  public boolean isEmpty() {
    return nulls.isEmpty();
  }

  /** Copy of the null rows as a {@link BitSet}. */
  public BitSet toBitSet() {
    return (BitSet) nulls.clone();
  }

  private static void checkRows(int rows) {
    if (rows < 0) {
      throw new IllegalArgumentException("rows must not be negative: " + rows);
    }
  }
}
