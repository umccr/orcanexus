package dev.ccgcm.platform.orcanexus.mockdb.seed;

import dev.ccgcm.platform.orcanexus.jooq.tables.WorkflowOutput;
import dev.ccgcm.platform.orcanexus.jooq.tables.records.WorkflowOutputRecord;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import org.jooq.Field;

/**
 * Seeds {@code workflow_output}: one row per workflow of the {@link SeedPlan}.
 *
 * <p>{@code portal_run_id} comes from workflow {@code j}. {@code cohort_id} is the cohort pool id
 * at the workflow's library index modulo the pool size, {@code bucket} the bucket pool id at row
 * modulo 3. Pool columns are never null. {@code prefix} is a free path {@code
 * mock/<cohort>/<portal_run_id>/}. {@code key_count} is a free BIGINT; row 0 holds 0 and row 1
 * holds {@code Long.MAX_VALUE} (protected from nulls) when the table is large enough.
 */
public final class WorkflowOutputSeeder implements TableSeeder {

  private static final String TABLE = "workflow_output";
  private static final int MAX_KEY_COUNT = 5000;

  @Override
  public String table() {
    return TABLE;
  }

  @Override
  public int rowCount(SeedPlan plan) {
    return plan.rowCount(TABLE);
  }

  @Override
  public Iterator<WorkflowOutputRecord> rows(
      SeedPlan plan, SeedContext context, RelationshipSpec spec, List<SeedNote> notes) {
    final int n = rowCount(plan);
    final var t = WorkflowOutput.WORKFLOW_OUTPUT;

    var placement = Boundaries.place(n, true);
    var boundaryRows = new BitSet(n);
    if (placement.fits()) {
      boundaryRows.set(placement.minRow());
      boundaryRows.set(placement.maxRow());
    } else {
      notes.add(
          new SeedNote(
              SeedNote.Kind.BOUNDARY_SKIPPED,
              TABLE,
              t.KEY_COUNT.getName(),
              TABLE + "." + t.KEY_COUNT.getName() + ": too few rows"));
    }

    Random nullRandom = context.stream("workflow_output.nulls");
    var nulls = new LinkedHashMap<Field<?>, NullPlan>();
    for (Field<?> field : t.fields()) {
      BitSet protectedRows = field.equals(t.KEY_COUNT) ? boundaryRows : null;
      nulls.put(field, NullPolicy.planFor(TABLE, field, spec, n, protectedRows, nullRandom));
    }
    final Random random = context.stream("workflow_output.values");
    final KeyPools pools = plan.pools();

    return new Iterator<>() {
      private int row;

      @Override
      public boolean hasNext() {
        return row < n;
      }

      @Override
      public WorkflowOutputRecord next() {
        if (row >= n) {
          throw new NoSuchElementException();
        }
        int j = row++;
        var r = new WorkflowOutputRecord();
        String portalRunId = plan.workflowPortalRunId(j);
        String cohort =
            pools.id(Pool.COHORT, plan.workflowLibraryIndex(j) % pools.size(Pool.COHORT));
        String bucket = pools.id(Pool.BUCKET, j % pools.size(Pool.BUCKET));
        // Always draw, so the stream does not depend on which values end up null.
        long keyCount = 1 + random.nextInt(MAX_KEY_COUNT);
        if (placement.fits() && j == placement.minRow()) {
          keyCount = Boundaries.bigintMin();
        } else if (placement.fits() && j == placement.maxRow()) {
          keyCount = Boundaries.bigintMax();
        }

        r.set(t.PORTAL_RUN_ID, portalRunId);
        r.set(t.COHORT_ID, cohort);
        r.set(t.BUCKET, bucket);
        put(r, nulls, j, t.PREFIX, "mock/" + cohort + "/" + portalRunId + "/");
        put(r, nulls, j, t.KEY_COUNT, keyCount);
        return r;
      }
    };
  }

  private static <T> void put(
      WorkflowOutputRecord record,
      Map<Field<?>, NullPlan> nulls,
      int row,
      Field<T> field,
      T value) {
    record.set(field, nulls.get(field).isNull(row) ? null : value);
  }
}
