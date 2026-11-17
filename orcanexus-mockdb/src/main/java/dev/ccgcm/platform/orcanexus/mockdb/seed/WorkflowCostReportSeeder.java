package dev.ccgcm.platform.orcanexus.mockdb.seed;

import dev.ccgcm.platform.orcanexus.jooq.tables.WorkflowCostReport;
import dev.ccgcm.platform.orcanexus.jooq.tables.records.WorkflowCostReportRecord;
import java.math.BigDecimal;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import org.jooq.Field;

/**
 * Seeds {@code workflow_cost_report}: one row per workflow of the {@link SeedPlan}.
 *
 * <p>{@code portal_run_id} is a pool column (never null). Name, version and status are copied from
 * the workflow plan and may be null under the null policy. {@code compute_cost} and {@code
 * license_cost} are random amounts and {@code total_cost} is their sum. {@code ica_project} is
 * {@code MOCK-ICA-nn} and {@code cost_unit} comes from a small vocabulary.
 *
 * <p>Boundary rows (protected from nulls for the three cost columns): row 0 holds 0.00, row 1 the
 * NUMERIC(18,2) maximum in all three. Row 1 is the deliberate exception where the total is not the
 * sum of its parts.
 */
public final class WorkflowCostReportSeeder implements TableSeeder {

  private static final String TABLE = "workflow_cost_report";
  private static final List<String> COST_UNITS = List.of("USD", "AUD", "iCredits");
  private static final int ICA_PROJECTS = 5;
  private static final int MAX_COMPUTE_CENTS = 5_000_000;
  private static final int MAX_LICENSE_CENTS = 500_000;

  @Override
  public String table() {
    return TABLE;
  }

  @Override
  public int rowCount(SeedPlan plan) {
    return plan.rowCount(TABLE);
  }

  @Override
  public Iterator<WorkflowCostReportRecord> rows(
      SeedPlan plan, SeedContext context, RelationshipSpec spec, List<SeedNote> notes) {
    final int n = rowCount(plan);
    final var t = WorkflowCostReport.WORKFLOW_COST_REPORT;
    final List<Field<BigDecimal>> costs = List.of(t.TOTAL_COST, t.COMPUTE_COST, t.LICENSE_COST);

    var placement = Boundaries.place(n, true);
    var boundaryRows = new BitSet(n);
    if (placement.fits()) {
      boundaryRows.set(placement.minRow());
      boundaryRows.set(placement.maxRow());
    } else {
      for (Field<?> f : costs) {
        notes.add(
            new SeedNote(
                SeedNote.Kind.BOUNDARY_SKIPPED,
                TABLE,
                f.getName(),
                TABLE + "." + f.getName() + ": too few rows"));
      }
    }
    final BigDecimal max = Boundaries.numericMax(t.TOTAL_COST).orElseThrow();
    final BigDecimal min = Boundaries.numericMin(t.TOTAL_COST).orElseThrow();

    Random nullRandom = context.stream("workflow_cost_report.nulls");
    var nulls = new LinkedHashMap<Field<?>, NullPlan>();
    for (Field<?> field : t.fields()) {
      BitSet protectedRows = costs.contains(field) ? boundaryRows : null;
      nulls.put(field, NullPolicy.planFor(TABLE, field, spec, n, protectedRows, nullRandom));
    }
    final Random random = context.stream("workflow_cost_report.values");

    return new Iterator<>() {
      private int row;

      @Override
      public boolean hasNext() {
        return row < n;
      }

      @Override
      public WorkflowCostReportRecord next() {
        if (row >= n) {
          throw new NoSuchElementException();
        }
        int j = row++;
        var r = new WorkflowCostReportRecord();
        // Always draw, so the stream does not depend on which values end up null.
        BigDecimal compute = BigDecimal.valueOf(random.nextInt(MAX_COMPUTE_CENTS), 2);
        BigDecimal license = BigDecimal.valueOf(random.nextInt(MAX_LICENSE_CENTS), 2);
        BigDecimal total = compute.add(license);
        String project = String.format("MOCK-ICA-%02d", random.nextInt(ICA_PROJECTS));
        String unit = COST_UNITS.get(random.nextInt(COST_UNITS.size()));
        if (placement.fits() && j == placement.minRow()) {
          compute = min;
          license = min;
          total = min;
        } else if (placement.fits() && j == placement.maxRow()) {
          compute = max;
          license = max;
          total = max;
        }

        r.set(t.PORTAL_RUN_ID, plan.workflowPortalRunId(j));
        put(r, nulls, j, t.WORKFLOW_NAME, plan.workflowName(j));
        put(r, nulls, j, t.WORKFLOW_VERSION, plan.workflowVersion(j));
        put(r, nulls, j, t.WORKFLOW_STATUS, plan.workflowStatus(j));
        put(r, nulls, j, t.TOTAL_COST, total);
        put(r, nulls, j, t.COMPUTE_COST, compute);
        put(r, nulls, j, t.LICENSE_COST, license);
        put(r, nulls, j, t.ICA_PROJECT, project);
        put(r, nulls, j, t.COST_UNIT, unit);
        return r;
      }
    };
  }

  private static <T> void put(
      WorkflowCostReportRecord record,
      Map<Field<?>, NullPlan> nulls,
      int row,
      Field<T> field,
      T value) {
    record.set(field, nulls.get(field).isNull(row) ? null : value);
  }
}
