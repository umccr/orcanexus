package dev.ccgcm.platform.orcanexus.mockdb.seed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableRecord;

/**
 * Seeds any table that has no dedicated seeder, from the jOOQ column types alone.
 *
 * <p>Row count is {@link SeedPlan#rowCount(String)} (default {@code max(L, 4)}). Pool columns named
 * by the spec take ids from their pool (row modulo pool size, never null) when the column is text.
 * Other columns get {@link GenericValues} with the null policy. Numeric, bigint, date and timestamp
 * columns get boundary rows (0 or minimum on row 0, maximum on row 1) when the table is large
 * enough, else a {@link SeedNote.Kind#BOUNDARY_SKIPPED} note. When the table has no spec entry, one
 * {@link SeedNote.Kind#NO_SPEC} note names it.
 */
public final class GenericSeeder implements TableSeeder {

  private final Table<?> table;

  public GenericSeeder(Table<?> table) {
    this.table = Objects.requireNonNull(table, "table");
  }

  @Override
  public String table() {
    return table.getName();
  }

  @Override
  public int rowCount(SeedPlan plan) {
    return plan.rowCount(table.getName());
  }

  @Override
  public Iterator<? extends TableRecord<?>> rows(
      SeedPlan plan, SeedContext context, RelationshipSpec spec, List<SeedNote> notes) {
    final String name = table.getName();
    final int n = rowCount(plan);
    if (!spec.hasEntry(name)) {
      notes.add(
          new SeedNote(
              SeedNote.Kind.NO_SPEC,
              name,
              null,
              name + ": no relationship spec entry, generated type-valid values"));
    }

    var placement = Boundaries.place(n, true);
    var boundaryRows = new BitSet(n);
    if (placement.fits()) {
      boundaryRows.set(placement.minRow());
      boundaryRows.set(placement.maxRow());
    }
    final Field<?>[] fields = table.fields();
    var nullRandom = context.stream(name + ".nulls");
    var nulls = new LinkedHashMap<Field<?>, NullPlan>();
    for (Field<?> field : fields) {
      boolean hasBoundary =
          !spec.isPoolColumn(name, field.getName()) && boundary(field, false).isPresent();
      if (hasBoundary && !placement.fits()) {
        notes.add(
            new SeedNote(
                SeedNote.Kind.BOUNDARY_SKIPPED,
                name,
                field.getName(),
                name + "." + field.getName() + ": too few rows"));
      }
      nulls.put(
          field,
          NullPolicy.planFor(
              name,
              field,
              spec,
              n,
              hasBoundary && placement.fits() ? boundaryRows : null,
              nullRandom));
    }
    final Random random = context.stream(name + ".values");
    final var pools = plan.pools();

    return new Iterator<TableRecord<?>>() {
      private int row;

      @Override
      public boolean hasNext() {
        return row < n;
      }

      @Override
      public TableRecord<?> next() {
        if (row >= n) {
          throw new NoSuchElementException();
        }
        int j = row++;
        TableRecord<?> record = (TableRecord<?>) table.newRecord();
        for (Field<?> field : fields) {
          Optional<Pool> pool = spec.poolOf(name, field.getName());
          // Always draw, so the stream does not depend on which values end up null.
          Object value = GenericValues.value(field.getDataType(), random, j);
          if (pool.isPresent() && field.getType() == String.class) {
            value = pools.id(pool.get(), j % pools.size(pool.get()));
          } else if (placement.fits() && j == placement.minRow()) {
            value = boundary(field, false).orElse(value);
          } else if (placement.fits() && j == placement.maxRow()) {
            value = boundary(field, true).orElse(value);
          }
          set(record, field, nulls.get(field).isNull(j) ? null : value);
        }
        return record;
      }
    };
  }

  private static <T> void set(TableRecord<?> record, Field<T> field, Object value) {
    record.set(field, value == null ? null : field.getDataType().convert(value));
  }

  private static Optional<Object> boundary(Field<?> field, boolean max) {
    Class<?> type = field.getType();
    if (type == BigDecimal.class) {
      return (max ? Boundaries.numericMax(field) : Boundaries.numericMin(field)).map(v -> v);
    }
    if (type == Long.class) {
      return Optional.of(max ? Boundaries.bigintMax() : Boundaries.bigintMin());
    }
    if (type == LocalDate.class) {
      return Optional.of(max ? Boundaries.dateMax() : Boundaries.dateMin());
    }
    if (type == LocalDateTime.class) {
      return Optional.of(max ? Boundaries.timestampMax() : Boundaries.timestampMin());
    }
    return Optional.empty();
  }
}
