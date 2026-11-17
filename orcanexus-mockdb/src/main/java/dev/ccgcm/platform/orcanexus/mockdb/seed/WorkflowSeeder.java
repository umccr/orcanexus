package dev.ccgcm.platform.orcanexus.mockdb.seed;

import dev.ccgcm.platform.orcanexus.jooq.tables.Workflow;
import dev.ccgcm.platform.orcanexus.jooq.tables.records.WorkflowRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import org.jooq.Field;

/**
 * Seeds {@code workflow}: one row per workflow of the {@link SeedPlan}.
 *
 * <p>{@code portal_run_id} (unique) and {@code library_id} (library {@code j mod L}) are pool
 * columns and never null. Name, version and status come from the plan vocabularies. {@code
 * workflow_end} is {@code workflow_start} plus the plan duration, and {@code workflow_duration} is
 * that duration in hours at scale 4; an end past {@link Boundaries#timestampMax()} is clamped to
 * it, so the seeded timestamp range holds. {@code workflow_comment} is {@code mock:} plus Datafaker
 * lorem text.
 *
 * <p>Boundary rows (always placed, since the table has at least 4 rows): row 0 holds the earliest
 * values ({@link Boundaries#timestampMin()} for start and end, duration 0), row 1 the latest
 * ({@link Boundaries#timestampMax()} for start and end, duration 99999999.9999). Row 0 is
 * consistent (end = start, duration 0). Row 1 is the deliberate exception: its duration is the
 * NUMERIC maximum and does not equal end minus start, because the timestamp range cannot hold that
 * many hours. Both rows are protected from nulls for the three columns involved.
 */
public final class WorkflowSeeder implements TableSeeder {

  private static final String TABLE = "workflow";
  private static final String COMMENT_PREFIX = "mock:";
  private static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3600);
  private static final int DURATION_SCALE = 4;

  @Override
  public String table() {
    return TABLE;
  }

  @Override
  public int rowCount(SeedPlan plan) {
    return plan.rowCount(TABLE);
  }

  @Override
  public Iterator<WorkflowRecord> rows(
      SeedPlan plan, SeedContext context, RelationshipSpec spec, List<SeedNote> notes) {
    final int n = rowCount(plan);

    var placement = Boundaries.place(n, true);
    final boolean boundaryFits = placement.fits();
    var boundaryRows = new BitSet(n);
    if (boundaryFits) {
      boundaryRows.set(placement.minRow());
      boundaryRows.set(placement.maxRow());
    } else {
      for (Field<?> f :
          List.of(
              Workflow.WORKFLOW.WORKFLOW_START,
              Workflow.WORKFLOW.WORKFLOW_END,
              Workflow.WORKFLOW.WORKFLOW_DURATION)) {
        String name = TABLE + "." + f.getName();
        notes.add(
            new SeedNote(
                SeedNote.Kind.BOUNDARY_SKIPPED, TABLE, f.getName(), name + ": too few rows"));
      }
    }
    final BigDecimal durationMax =
        Boundaries.numericMax(Workflow.WORKFLOW.WORKFLOW_DURATION).orElseThrow();
    final BigDecimal durationMin =
        Boundaries.numericMin(Workflow.WORKFLOW.WORKFLOW_DURATION).orElseThrow();

    Random nullRandom = context.stream("workflow.nulls");
    var nulls = new LinkedHashMap<Field<?>, NullPlan>();
    for (Field<?> field : Workflow.WORKFLOW.fields()) {
      BitSet protectedRows =
          field.equals(Workflow.WORKFLOW.WORKFLOW_START)
                  || field.equals(Workflow.WORKFLOW.WORKFLOW_END)
                  || field.equals(Workflow.WORKFLOW.WORKFLOW_DURATION)
              ? boundaryRows
              : null;
      nulls.put(field, NullPolicy.planFor(TABLE, field, spec, n, protectedRows, nullRandom));
    }

    var faker = context.faker(context.stream("workflow.values"));

    return new Iterator<>() {
      private int row;

      @Override
      public boolean hasNext() {
        return row < n;
      }

      @Override
      public WorkflowRecord next() {
        if (row >= n) {
          throw new NoSuchElementException();
        }
        int j = row++;
        var r = new WorkflowRecord();

        LocalDateTime start = plan.workflowStartLocal(j);
        LocalDateTime end = start.plus(plan.workflowDuration(j));
        if (end.isAfter(Boundaries.timestampMax())) {
          end = Boundaries.timestampMax(); // keep the seeded timestamp range
        }
        BigDecimal hours =
            BigDecimal.valueOf(plan.workflowDuration(j).toSeconds())
                .divide(SECONDS_PER_HOUR, DURATION_SCALE, RoundingMode.HALF_UP);
        if (boundaryFits && j == placement.minRow()) {
          start = Boundaries.timestampMin();
          end = start;
          hours = durationMin;
        } else if (boundaryFits && j == placement.maxRow()) {
          start = Boundaries.timestampMax();
          end = start;
          hours = durationMax;
        }
        // Always draw, so the stream does not depend on which values end up null.
        String comment = COMMENT_PREFIX + faker.lorem().sentence();

        // Pool columns: never null.
        r.set(Workflow.WORKFLOW.PORTAL_RUN_ID, plan.workflowPortalRunId(j));
        r.set(Workflow.WORKFLOW.LIBRARY_ID, plan.workflowLibraryId(j));
        // Free columns.
        put(r, nulls, j, Workflow.WORKFLOW.WORKFLOW_NAME, plan.workflowName(j));
        put(r, nulls, j, Workflow.WORKFLOW.WORKFLOW_VERSION, plan.workflowVersion(j));
        put(r, nulls, j, Workflow.WORKFLOW.WORKFLOW_STATUS, plan.workflowStatus(j));
        put(r, nulls, j, Workflow.WORKFLOW.WORKFLOW_START, start);
        put(r, nulls, j, Workflow.WORKFLOW.WORKFLOW_END, end);
        put(r, nulls, j, Workflow.WORKFLOW.WORKFLOW_DURATION, hours);
        put(r, nulls, j, Workflow.WORKFLOW.WORKFLOW_COMMENT, comment);
        return r;
      }
    };
  }

  private static <T> void put(
      WorkflowRecord record, Map<Field<?>, NullPlan> nulls, int row, Field<T> field, T value) {
    record.set(field, nulls.get(field).isNull(row) ? null : value);
  }
}
