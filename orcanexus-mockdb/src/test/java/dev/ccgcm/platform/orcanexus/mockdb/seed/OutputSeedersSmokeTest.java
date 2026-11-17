package dev.ccgcm.platform.orcanexus.mockdb.seed;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccgcm.platform.orcanexus.jooq.tables.WorkflowCostReport;
import dev.ccgcm.platform.orcanexus.jooq.tables.WorkflowOutput;
import dev.ccgcm.platform.orcanexus.jooq.tables.records.WorkflowCostReportRecord;
import dev.ccgcm.platform.orcanexus.jooq.tables.records.WorkflowOutputRecord;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutputSeedersSmokeTest {

  private static <R extends org.jooq.TableRecord<?>> List<R> seed(
      TableSeeder seeder, int scale, List<SeedNote> notes) {
    var ctx = new SeedContext(scale, 42);
    var rows = new ArrayList<R>();
    @SuppressWarnings("unchecked")
    var it =
        (java.util.Iterator<R>)
            seeder.rows(SeedPlan.of(ctx), ctx, RelationshipSpec.defaults(), notes);
    it.forEachRemaining(rows::add);
    return rows;
  }

  @Test
  void workflowOutputHasPoolsAndBoundaries() {
    List<WorkflowOutputRecord> rows = seed(new WorkflowOutputSeeder(), 100, new ArrayList<>());
    var t = WorkflowOutput.WORKFLOW_OUTPUT;
    assertThat(rows).hasSize(200);
    assertThat(new HashSet<>(rows.stream().map(r -> r.get(t.PORTAL_RUN_ID)).toList())).hasSize(200);
    assertThat(rows)
        .allMatch(
            r ->
                r.get(t.COHORT_ID) != null
                    && r.get(t.BUCKET) != null
                    && r.get(t.PORTAL_RUN_ID) != null);
    assertThat(rows.get(0).get(t.KEY_COUNT)).isZero();
    assertThat(rows.get(1).get(t.KEY_COUNT)).isEqualTo(Long.MAX_VALUE);
    assertThat(rows.stream().filter(r -> r.get(t.PREFIX) == null).count()).isPositive();
  }

  @Test
  void costReportHasBoundariesAndNulls() {
    List<WorkflowCostReportRecord> rows =
        seed(new WorkflowCostReportSeeder(), 100, new ArrayList<>());
    var t = WorkflowCostReport.WORKFLOW_COST_REPORT;
    assertThat(rows).hasSize(200);
    assertThat(rows.get(0).get(t.TOTAL_COST)).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(rows.get(1).get(t.LICENSE_COST)).isEqualByComparingTo("9999999999999999.99");
    assertThat(rows.stream().filter(r -> r.get(t.ICA_PROJECT) == null).count()).isPositive();
    assertThat(rows).allMatch(r -> r.get(t.PORTAL_RUN_ID) != null);
  }

  @Test
  void tinyScaleFitsBoth() {
    assertThat(seed(new WorkflowOutputSeeder(), 1, new ArrayList<>())).hasSize(4);
    assertThat(seed(new WorkflowCostReportSeeder(), 1, new ArrayList<>())).hasSize(4);
  }
}
