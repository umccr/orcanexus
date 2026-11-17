package dev.ccgcm.platform.orcanexus.mockdb.seed;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccgcm.platform.orcanexus.mockdb.cli.Output;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

class SeedRunnerTest {

  private static final class Capture implements Output {
    final List<String> out = new ArrayList<>();
    final List<String> err = new ArrayList<>();

    @Override
    public void out(String line) {
      out.add(line);
    }

    @Override
    public void err(String line) {
      err.add(line);
    }
  }

  /** Records statements; {@code existsFor} makes the emptiness check of that table non-empty. */
  private static final class Db implements MockDataProvider {
    final List<String> sql = new ArrayList<>();
    final List<Integer> batchSizes = new ArrayList<>();
    final String existsFor;
    final boolean missing;

    Db(String existsFor, boolean missing) {
      this.existsFor = existsFor;
      this.missing = missing;
    }

    @Override
    public MockResult[] execute(org.jooq.tools.jdbc.MockExecuteContext ctx) throws SQLException {
      String s = ctx.sql();
      sql.add(s);
      var create = DSL.using(SQLDialect.POSTGRES);
      if (s.toLowerCase(java.util.Locale.ROOT).contains("exists")) {
        if (missing) {
          throw new SQLException("relation does not exist", "42P01");
        }
        var field = DSL.field("x", Boolean.class);
        var result = create.newResult(field);
        boolean nonEmpty = existsFor != null && s.contains("\"" + existsFor + "\"");
        result.add(create.newRecord(field).values(nonEmpty));
        return new MockResult[] {new MockResult(result.size(), result)};
      }
      if (s.toLowerCase(java.util.Locale.ROOT).startsWith("insert")) {
        int n = ctx.batchBindings() == null ? 1 : ctx.batchBindings().length;
        batchSizes.add(n);
        var results = new MockResult[n];
        for (int i = 0; i < n; i++) {
          results[i] = new MockResult(1);
        }
        return results;
      }
      return new MockResult[] {new MockResult(0)};
    }
  }

  private static org.jooq.DSLContext dsl(Db db) {
    return DSL.using(new MockConnection(db), SQLDialect.POSTGRES);
  }

  @Test
  void insertsInBatchesOfAtMost1000AndReportsCounts() {
    var db = new Db(null, false);
    var out = new Capture();
    SeedResult result = new SeedRunner().seed(dsl(db), out, 600, 42);
    assertThat(result.exitCode()).isZero();
    assertThat(result.rowCounts().keySet())
        .containsExactly(
            "lims", "workflow", "fastq", "bam", "vcf", "workflow_output", "workflow_cost_report");
    assertThat(result.rowCounts()).containsEntry("lims", 600).containsEntry("workflow", 1200);
    assertThat(db.batchSizes).isNotEmpty().allMatch(n -> n <= SeedRunner.BATCH_SIZE);
    assertThat(db.batchSizes.stream().mapToInt(Integer::intValue).sum())
        .isEqualTo((int) result.totalRows());
    assertThat(db.batchSizes).contains(1000);
  }

  @Test
  void refusesNonEmptyTablesAndInsertsNothing() {
    var db = new Db("bam", false);
    var out = new Capture();
    SeedResult result = new SeedRunner().seed(dsl(db), out, 10, 42);
    assertThat(result.exitCode()).isEqualTo(1);
    assertThat(out.err).anyMatch(l -> l.contains("bam") && l.contains("tables are not empty"));
    assertThat(db.batchSizes).isEmpty();
  }

  @Test
  void missingTableSuggestsSchema() {
    var db = new Db(null, true);
    var out = new Capture();
    assertThat(new SeedRunner().seed(dsl(db), out, 10, 42).exitCode()).isEqualTo(1);
    assertThat(out.err).anyMatch(l -> l.contains("run `schema` first") || l.contains("`schema`"));
    assertThat(db.batchSizes).isEmpty();
  }

  @Test
  void specMismatchIsReportedBeforeAnyStatement() {
    var db = new Db(null, false);
    var out = new Capture();
    var spec = RelationshipSpec.of(Map.of("lims", Map.of("no_such_column", Pool.LIBRARY)));
    assertThat(new SeedRunner(spec).seed(dsl(db), out, 10, 42).exitCode()).isEqualTo(1);
    assertThat(out.err).anyMatch(l -> l.contains("lims") && l.contains("no_such_column"));
    assertThat(db.sql).isEmpty();
  }
}
