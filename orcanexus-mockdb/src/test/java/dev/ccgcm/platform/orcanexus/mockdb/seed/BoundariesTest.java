package dev.ccgcm.platform.orcanexus.mockdb.seed;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccgcm.platform.orcanexus.jooq.tables.Workflow;
import dev.ccgcm.platform.orcanexus.jooq.tables.WorkflowCostReport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BoundariesTest {

  @Test
  void numeric12x4Boundaries() {
    var field = Workflow.WORKFLOW.WORKFLOW_DURATION;
    assertThat(Boundaries.hasNumericBoundary(field)).isTrue();
    assertThat(Boundaries.numericMax(field)).contains(new BigDecimal("99999999.9999"));
    assertThat(Boundaries.numericMin(field)).contains(new BigDecimal("0.0000"));
    assertThat(Boundaries.numericMin(field).orElseThrow().scale()).isEqualTo(4);
    assertThat(Boundaries.numericMax(field).orElseThrow().scale()).isEqualTo(4);
  }

  @Test
  void numeric18x2Boundaries() {
    var field = WorkflowCostReport.WORKFLOW_COST_REPORT.TOTAL_COST;
    assertThat(Boundaries.numericMax(field)).contains(new BigDecimal("9999999999999999.99"));
    assertThat(Boundaries.numericMin(field)).contains(new BigDecimal("0.00"));
    assertThat(Boundaries.numericMin(field).orElseThrow().scale()).isEqualTo(2);
  }

  @Test
  void numericWithoutPrecisionHasNoBoundary() {
    var field = DSL.field(DSL.name("x"), SQLDataType.NUMERIC);
    assertThat(Boundaries.hasNumericBoundary(field)).isFalse();
    assertThat(Boundaries.numericMin(field)).isEmpty();
    assertThat(Boundaries.numericMax(field)).isEmpty();
  }

  @Test
  void numericMaxHandlesZeroScale() {
    assertThat(Boundaries.numericMax(5, 0)).isEqualTo(new BigDecimal("99999"));
  }

  @Test
  void bigintBoundaries() {
    assertThat(Boundaries.bigintMin()).isZero();
    assertThat(Boundaries.bigintMax()).isEqualTo(Long.MAX_VALUE);
  }

  @Test
  void dateAndTimestampBoundariesMatchSeedRange() {
    assertThat(Boundaries.dateMin()).isEqualTo(SeedPlan.RANGE_START);
    assertThat(Boundaries.dateMax()).isEqualTo(SeedPlan.RANGE_END);
    assertThat(Boundaries.timestampMin()).isEqualTo(SeedPlan.RANGE_START.atStartOfDay());
    var max = Boundaries.timestampMax();
    assertThat(max.toLocalDate()).isEqualTo(SeedPlan.RANGE_END);
    assertThat(max)
        .isEqualTo(
            LocalDateTime.of(LocalDate.of(2025, 12, 31), java.time.LocalTime.of(23, 59, 59)));
  }

  @ParameterizedTest
  @CsvSource({
    "0,true,false",
    "1,true,false",
    "2,true,false",
    "3,true,true",
    "4,true,true",
    "0,false,false",
    "1,false,false",
    "2,false,true",
    "3,false,true"
  })
  void placeFollowsMinimumRowRule(int rows, boolean needsNull, boolean fits) {
    var placement = Boundaries.place(rows, needsNull);
    assertThat(placement.fits()).isEqualTo(fits);
    if (fits) {
      assertThat(placement.minRow()).isZero();
      assertThat(placement.maxRow()).isEqualTo(1);
      assertThat(placement.maxRow()).isLessThan(rows);
    }
  }
}
