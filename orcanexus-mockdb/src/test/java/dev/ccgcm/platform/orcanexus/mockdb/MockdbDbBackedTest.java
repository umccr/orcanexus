package dev.ccgcm.platform.orcanexus.mockdb;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccgcm.platform.orcanexus.jooq.Mart;
import dev.ccgcm.platform.orcanexus.mockdb.cli.MockdbCli;
import dev.ccgcm.platform.orcanexus.mockdb.cli.RealCommandActions;
import dev.ccgcm.platform.orcanexus.mockdb.db.SchemaService;
import dev.ccgcm.platform.orcanexus.mockdb.seed.SeedRunner;
import dev.ccgcm.platform.orcanexus.mockdb.testsupport.RecordingOutput;
import dev.ccgcm.platform.orcanexus.mockdb.testsupport.ScratchDatabaseExtension;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * DB-backed acceptance tests against a scratch database; enabled only with {@code -Pmockdb-it}.
 * Validates: Requirements 4.10, 5.1, 5.4, 5.6, 5.9, 7.5, 7.6, 9.1, 9.4, 9.5; Properties 9 (catalog
 * half) and 15.
 */
@EnabledIfSystemProperty(named = "mockdb.it", matches = "true")
class MockdbDbBackedTest {

  @RegisterExtension static final ScratchDatabaseExtension DB = new ScratchDatabaseExtension();

  private static final String SCHEMA = Mart.MART.getName();
  private static final Pattern CREATE_TABLE =
      Pattern.compile("create table if not exists \"[^\"]+\"\\.\"([^\"]+)\".*", Pattern.DOTALL);

  private final SchemaService schemaService = new SchemaService();
  private final SeedRunner seedRunner = new SeedRunner();
  private final RecordingOutput output = new RecordingOutput();

  @BeforeEach
  void dropModelSchema() {
    DB.dsl().execute("drop schema if exists {0} cascade", DSL.name(SCHEMA));
  }

  private int schema() throws SQLException {
    try (var conn = DB.database().open()) {
      return schemaService.schema(conn, output);
    }
  }

  private int reset() throws SQLException {
    try (var conn = DB.database().open()) {
      return schemaService.reset(conn, output);
    }
  }

  private int seed(int scale) throws SQLException {
    try (var conn = DB.database().open()) {
      return seedRunner.run(conn, output, scale, 42);
    }
  }

  private static Table<?> table(String name) {
    return Mart.MART.getTables().stream()
        .filter(t -> t.getName().equals(name))
        .findFirst()
        .orElseThrow();
  }

  private static long count(DSLContext dsl, Table<?> table) {
    return dsl.fetchCount(table);
  }

  private static Map<String, Long> counts(DSLContext dsl) {
    var result = new LinkedHashMap<String, Long>();
    for (Table<?> t : Mart.MART.getTables()) {
      result.put(t.getName(), count(dsl, t));
    }
    return result;
  }

  private static List<String> tableNamesInDatabase() {
    return DB.dsl()
        .fetch(
            "select table_name from information_schema.tables where table_schema = ?"
                + " order by table_name",
            SCHEMA)
        .getValues(0, String.class);
  }

  // (a) DDL acceptance, idempotence, rollback

  @Test
  void schemaOnEmptyDatabaseCreatesAllTables() throws SQLException {
    assertThat(schema()).isZero();
    assertThat(tableNamesInDatabase())
        .hasSize(7)
        .containsExactlyInAnyOrderElementsOf(
            Mart.MART.getTables().stream().map(Table::getName).toList());
  }

  @Test
  void schemaTwiceKeepsExistingRows() throws SQLException {
    assertThat(schema()).isZero();
    var lims = table("lims");
    var dsl = DB.dsl();
    dsl.insertInto(lims, lims.field("library_id", String.class)).values("L9999999").execute();

    assertThat(schema()).isZero();

    assertThat(count(dsl, lims)).isEqualTo(1);
    assertThat(dsl.select(lims.field("library_id")).from(lims).fetchOne(0)).isEqualTo("L9999999");
    assertThat(tableNamesInDatabase()).hasSize(7);
  }

  /**
   * Rollback case: a stand-alone enum type named like the last table is created beforehand. {@code
   * create table if not exists} only checks relations, and creating a table also creates a
   * composite type of the same name, so that statement fails with "type already exists" after the
   * earlier tables were created in the same transaction. Nothing may remain.
   */
  @Test
  void failingStatementRollsBackEarlierTables() throws SQLException {
    var statements = schemaService.ddlStatements();
    var last = statements.get(statements.size() - 1);
    var matcher = CREATE_TABLE.matcher(last.strip());
    assertThat(matcher.matches()).as("last statement is a create table").isTrue();
    var lastTable = matcher.group(1);
    var dsl = DB.dsl();
    dsl.execute("create schema {0}", DSL.name(SCHEMA));
    dsl.execute("create type {0} as enum ('x')", DSL.name(SCHEMA, lastTable));

    int rc = schema();

    assertThat(rc).isEqualTo(1);
    assertThat(output.allErr())
        .contains("Failing statement:")
        .contains("create table if not exists")
        .contains(lastTable)
        .contains("Database error:");
    assertThat(tableNamesInDatabase()).isEmpty();
  }

  // (b) Property 9, catalog half

  @Test
  void catalogMatchesModelColumns() throws SQLException {
    assertThat(schema()).isZero();
    var dsl = DB.dsl();

    for (Table<?> expected : Mart.MART.getTables()) {
      var rows =
          dsl.fetch(
              "select column_name, data_type, is_nullable, numeric_precision, numeric_scale,"
                  + " datetime_precision from information_schema.columns"
                  + " where table_schema = ? and table_name = ? order by ordinal_position",
              SCHEMA,
              expected.getName());
      assertThat(rows.getValues("column_name", String.class))
          .as(expected.getName() + " columns")
          .containsExactlyElementsOf(Arrays.stream(expected.fields()).map(Field::getName).toList());
      for (var row : rows) {
        var field = expected.field(row.get("column_name", String.class));
        var where = expected.getName() + "." + field.getName();
        var type = field.getDataType();
        assertThat(row.get("data_type", String.class))
            .as(where + " type")
            .isEqualTo(catalogType(type.getType()));
        assertThat("YES".equals(row.get("is_nullable", String.class)))
            .as(where + " nullable")
            .isEqualTo(type.nullable());
        if (type.getType() == BigDecimal.class) {
          assertThat(row.get("numeric_precision", Integer.class))
              .as(where + " precision")
              .isEqualTo(type.precision());
          assertThat(row.get("numeric_scale", Integer.class))
              .as(where + " scale")
              .isEqualTo(type.scale());
        } else if (type.getType() == LocalDateTime.class) {
          assertThat(row.get("datetime_precision", Integer.class))
              .as(where + " timestamp precision")
              .isEqualTo(type.precision());
        }
      }
    }
  }

  private static String catalogType(Class<?> javaType) {
    if (javaType == String.class) {
      return "text";
    } else if (javaType == Long.class) {
      return "bigint";
    } else if (javaType == LocalDate.class) {
      return "date";
    } else if (javaType == LocalDateTime.class) {
      return "timestamp without time zone";
    } else if (javaType == BigDecimal.class) {
      return "numeric";
    }
    throw new AssertionError("No catalog mapping for " + javaType);
  }

  // (c) reset

  @Test
  void resetAfterSeedLeavesSchemaWithZeroRows() throws SQLException {
    assertThat(schema()).isZero();
    assertThat(seed(10)).isZero();
    assertThat(counts(DB.dsl()).values()).anyMatch(n -> n > 0);

    assertThat(reset()).isZero();

    assertThat(tableNamesInDatabase()).hasSize(7);
    assertThat(counts(DB.dsl()).values()).allMatch(n -> n == 0);
  }

  @Test
  void resetWithoutSchemaSucceeds() throws SQLException {
    assertThat(reset()).isZero();
    assertThat(tableNamesInDatabase()).hasSize(7);
  }

  // (d) seed refusals

  @Test
  void seedTwiceRefusesSecondRunAndKeepsCounts() throws SQLException {
    assertThat(schema()).isZero();
    assertThat(seed(10)).isZero();
    var before = counts(DB.dsl());

    var second = new RecordingOutput();
    int rc;
    try (var conn = DB.database().open()) {
      rc = seedRunner.run(conn, second, 10, 42);
    }

    assertThat(rc).isEqualTo(1);
    assertThat(second.allErr()).contains("not empty");
    assertThat(counts(DB.dsl())).isEqualTo(before);
  }

  @Test
  void seedWithoutSchemaHintsAndInsertsNothing() throws SQLException {
    int rc = seed(10);

    assertThat(rc).isEqualTo(1);
    assertThat(output.allErr()).contains("Run `schema` first");
    assertThat(tableNamesInDatabase()).isEmpty();
  }

  // (e) joins

  @ParameterizedTest
  @ValueSource(strings = {"fastq", "bam", "vcf", "workflow"})
  void limsJoinsOnLibraryIdReturnRows(String other) throws SQLException {
    seedAtScale100();
    assertThat(joinCount("lims", other, "library_id")).as("lims x " + other).isPositive();
  }

  @ParameterizedTest
  @ValueSource(strings = {"fastq", "bam", "vcf", "workflow_output", "workflow_cost_report"})
  void workflowJoinsOnPortalRunIdReturnRows(String other) throws SQLException {
    seedAtScale100();
    assertThat(joinCount("workflow", other, "portal_run_id"))
        .as("workflow x " + other)
        .isPositive();
  }

  private void seedAtScale100() throws SQLException {
    assertThat(schema()).isZero();
    assertThat(seed(100)).isZero();
  }

  private static int joinCount(String left, String right, String column) {
    var l = table(left).as("l");
    var r = table(right).as("r");
    return DB.dsl()
        .fetchCount(
            DSL.selectOne()
                .from(l)
                .join(r)
                .on(DSL.field(DSL.name("l", column)).eq(DSL.field(DSL.name("r", column)))));
  }

  // (f) Property 15: up is idempotent

  @ParameterizedTest
  @CsvSource({"1", "10", "100"})
  void upTwiceGivesIdenticalContents(int scale) {
    var first = runUp(scale);
    var firstCounts = counts(DB.dsl());
    var firstContents = contents();

    var second = runUp(scale);

    assertThat(first).isZero();
    assertThat(second).isZero();
    assertThat(counts(DB.dsl())).isEqualTo(firstCounts);
    assertThat(contents()).isEqualTo(firstContents);
    assertThat(firstCounts.values()).allMatch(n -> n > 0);
  }

  private int runUp(int scale) {
    var config = DB.config();
    var env =
        Map.of(
            "MOCKDB_HOST", config.host(),
            "MOCKDB_PORT", String.valueOf(config.port()),
            "MOCKDB_NAME", config.database(),
            "MOCKDB_USER", config.user(),
            "MOCKDB_PASSWORD", config.password());
    return new MockdbCli(new RealCommandActions())
        .run(
            new String[] {"up", "--scale", String.valueOf(scale), "--seed", "7"},
            env,
            new RecordingOutput(),
            DB.connector());
  }

  /** Every table, rows ordered by all columns, as plain lists. */
  private static Map<String, List<List<Object>>> contents() {
    var result = new LinkedHashMap<String, List<List<Object>>>();
    for (Table<?> t : Mart.MART.getTables()) {
      var rows = new ArrayList<List<Object>>();
      for (var record : DB.dsl().selectFrom(t).orderBy(t.fields()).fetch()) {
        rows.add(Arrays.asList(record.intoArray()));
      }
      result.put(t.getName(), rows);
    }
    return result;
  }
}
