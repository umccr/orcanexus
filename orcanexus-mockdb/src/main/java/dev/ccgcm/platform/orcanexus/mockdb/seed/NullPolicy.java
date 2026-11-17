package dev.ccgcm.platform.orcanexus.mockdb.seed;

import java.util.BitSet;
import java.util.Random;
import org.jooq.Field;

/** Applies the null policy to one column: pool and non-nullable columns never get nulls. */
public final class NullPolicy {

  private NullPolicy() {}

  /**
   * Null plan for a column.
   *
   * @param table table name
   * @param field the column
   * @param spec relationship spec; a pool column gets no nulls
   * @param rows row count of the table
   * @param protectedRows rows that must stay non-null (boundary and tie rows); may be null
   * @param random the table's stream; not touched for pool and non-nullable columns
   */
  public static NullPlan planFor(
      String table,
      Field<?> field,
      RelationshipSpec spec,
      int rows,
      BitSet protectedRows,
      Random random) {
    if (spec.isPoolColumn(table, field.getName()) || !field.getDataType().nullable()) {
      return NullPlan.none(rows);
    }
    return NullPlan.of(rows, protectedRows, random);
  }
}
