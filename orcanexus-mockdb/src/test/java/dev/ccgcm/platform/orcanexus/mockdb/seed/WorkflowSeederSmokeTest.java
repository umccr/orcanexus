package dev.ccgcm.platform.orcanexus.mockdb.seed;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccgcm.platform.orcanexus.jooq.tables.Workflow;
import dev.ccgcm.platform.orcanexus.jooq.tables.records.WorkflowRecord;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkflowSeederSmokeTest {

  private static List<WorkflowRecord> seed(int scale) {
    var ctx = new SeedContext(scale, 42);
    var rows = new ArrayList<WorkflowRecord>();
    new WorkflowSeeder()
        .rows(SeedPlan.of(ctx), ctx, RelationshipSpec.defaults(), new ArrayList<>())
        .forEachRemaining(rows::add);
    return rows;
  }

  @Test
  void yieldsUniquePortalRunsAndBoundaryRows() {
    var rows = seed(100);
    assertThat(rows).hasSize(200);
    assertThat(
            new HashSet<>(rows.stream().map(r -> r.get(Workflow.WORKFLOW.PORTAL_RUN_ID)).toList()))
        .hasSize(200);
    assertThat(rows).allMatch(r -> r.get(Workflow.WORKFLOW.LIBRARY_ID) != null);
    assertThat(rows.get(0).get(Workflow.WORKFLOW.WORKFLOW_DURATION))
        .isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(rows.get(1).get(Workflow.WORKFLOW.WORKFLOW_DURATION))
        .isEqualByComparingTo("99999999.9999");
    assertThat(rows.get(0).get(Workflow.WORKFLOW.WORKFLOW_START))
        .isEqualTo(Boundaries.timestampMin());
    assertThat(rows.get(1).get(Workflow.WORKFLOW.WORKFLOW_END))
        .isEqualTo(Boundaries.timestampMax());
    assertThat(rows.get(5).get(Workflow.WORKFLOW.WORKFLOW_COMMENT)).isNull();
  }

  @Test
  void tinyScaleStillFits() {
    assertThat(seed(1)).hasSize(4);
  }
}
