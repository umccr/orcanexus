package dev.ccgcm.platform.orcanexus.mockdb.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.ccgcm.platform.orcanexus.jooq.Mart;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jooq.Table;
import org.junit.jupiter.api.Test;

/** Property 11: Relationship_Spec matches the model (Requirements 7.2, 7.7, 13.6). */
class RelationshipSpecTest {

  private final RelationshipSpec spec = RelationshipSpec.defaults();

  @Test
  void defaultsValidateAgainstModel() {
    spec.validate(Mart.MART);
  }

  @Test
  void everySpecTableAndColumnExistsInModel() {
    for (String table : spec.tables()) {
      Table<?> t = Mart.MART.getTable(table);
      assertThat(t).as("table %s", table).isNotNull();
      for (String column : spec.columnsOf(table).keySet()) {
        assertThat(t.field(column)).as("column %s.%s", table, column).isNotNull();
      }
    }
  }

  @Test
  void missingColumnFailsNamingTableAndColumn() {
    var bad = RelationshipSpec.of(Map.of("lims", Map.of("no_such_column", Pool.LIBRARY)));
    assertThatThrownBy(() -> bad.validate(Mart.MART))
        .isInstanceOf(SpecMismatchException.class)
        .hasMessageContaining("lims")
        .hasMessageContaining("no_such_column");
  }

  @Test
  void missingTableFailsNamingTable() {
    var bad = RelationshipSpec.of(Map.of("no_such_table", Map.of("library_id", Pool.LIBRARY)));
    assertThatThrownBy(() -> bad.validate(Mart.MART))
        .isInstanceOf(SpecMismatchException.class)
        .hasMessageContaining("no_such_table");
  }

  @Test
  void queriesReportPoolsAndEntries() {
    assertThat(spec.poolOf("lims", "library_id")).contains(Pool.LIBRARY);
    assertThat(spec.poolOf("workflow", "portal_run_id")).contains(Pool.PORTAL_RUN);
    assertThat(spec.poolOf("lims", "nope")).isEmpty();
    assertThat(spec.poolOf("nope", "library_id")).isEmpty();
    assertThat(spec.isPoolColumn("fastq", "cohort_id")).isTrue();
    assertThat(spec.isPoolColumn("fastq", "nope")).isFalse();
    assertThat(spec.hasEntry("bam")).isTrue();
    assertThat(spec.hasEntry("nope")).isFalse();
    assertThat(spec.tables())
        .containsExactly(
            "lims", "fastq", "bam", "vcf", "workflow", "workflow_output", "workflow_cost_report");
    assertThat(spec.columnsOf("workflow_cost_report"))
        .containsExactly(Map.entry("portal_run_id", Pool.PORTAL_RUN));
    assertThat(spec.columnsOf("nope")).isEmpty();
  }

  @Test
  void specIsImmutable() {
    var columns = new LinkedHashMap<String, Pool>();
    columns.put("library_id", Pool.LIBRARY);
    var source = new LinkedHashMap<String, Map<String, Pool>>();
    source.put("lims", columns);
    var built = RelationshipSpec.of(source);

    columns.put("sample_id", Pool.SAMPLE);
    source.put("fastq", Map.of("library_id", Pool.LIBRARY));

    assertThat(built.tables()).containsExactly("lims");
    assertThat(built.columnsOf("lims")).containsOnlyKeys("library_id");

    assertThatThrownBy(() -> built.columnsOf("lims").put("x", Pool.SAMPLE))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> built.tables().add("x"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> built.columnsOf("nope").put("x", Pool.SAMPLE))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void keyColumnsAreMappedWhereverModelHasThem() {
    assertKeyColumn("library_id", Pool.LIBRARY);
    assertKeyColumn("portal_run_id", Pool.PORTAL_RUN);
  }

  private void assertKeyColumn(String column, Pool pool) {
    List<String> withColumn =
        Mart.MART.getTables().stream()
            .filter(t -> t.field(column) != null)
            .map(Table::getName)
            .toList();
    assertThat(withColumn).as("model tables having %s", column).isNotEmpty();
    for (String table : withColumn) {
      assertThat(spec.poolOf(table, column)).as("%s.%s", table, column).contains(pool);
    }
  }

  @Test
  void everyMartTableHasSpecEntry() {
    Set<String> unspecified = new LinkedHashSet<>();
    Mart.MART.getTables().forEach(t -> unspecified.add(t.getName()));
    unspecified.removeAll(spec.tables());
    assertThat(unspecified).as("Mart tables without a spec entry").isEmpty();
  }
}
