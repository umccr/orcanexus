package dev.ccgcm.platform.orcanexus.mockdb.seed;

import dev.ccgcm.platform.orcanexus.jooq.Mart;
import dev.ccgcm.platform.orcanexus.mockdb.cli.Output;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.TableRecord;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

/**
 * Seeds every relation of the model in one transaction.
 *
 * <p>Order of work: validate the relationship spec (nothing touches the database yet), check that
 * every relation is empty, then insert lazily in batches of {@value #BATCH_SIZE}. Any failure rolls
 * the transaction back, so all tables stay empty.
 */
public final class SeedRunner {

  /** Rows per {@code batchInsert}. */
  public static final int BATCH_SIZE = 1000;

  private static final String MISSING_TABLE = "42P01";
  private static final int OK = 0;
  private static final int FAILED = 1;

  /** Insertion order that keeps joins meaningful; tables not listed come last, in model order. */
  private static final List<String> ORDER =
      List.of("lims", "workflow", "fastq", "bam", "vcf", "workflow_output", "workflow_cost_report");

  private final RelationshipSpec spec;

  /** A runner using {@link RelationshipSpec#defaults()}. */
  public SeedRunner() {
    this(RelationshipSpec.defaults());
  }

  /** A runner using the given spec (tests use this to provoke a mismatch). */
  public SeedRunner(RelationshipSpec spec) {
    this.spec = Objects.requireNonNull(spec, "spec");
  }

  /** Seeds the database and prints the summary. Returns the process exit code. */
  public int run(Connection connection, Output out, int scale, int seed) {
    SeedResult result = seed(DSL.using(connection, SQLDialect.POSTGRES), out, scale, seed);
    if (result.exitCode() != OK) {
      return result.exitCode();
    }
    printSummary(result, out);
    return OK;
  }

  /**
   * Testable core. Prints errors to {@code out}; the summary is printed by {@link #run}.
   *
   * @return the result; {@code exitCode} 1 and no counts on failure
   */
  public SeedResult seed(DSLContext dsl, Output out, int scale, int seed) {
    try {
      spec.validate(Mart.MART);
    } catch (SpecMismatchException e) {
      out.err(
          "Relationship spec does not match the model: table '"
              + e.table()
              + "'"
              + (e.column() == null ? "" : ", column '" + e.column() + "'")
              + ". Nothing was inserted.");
      return SeedResult.failure();
    }
    var context = new SeedContext(scale, seed);
    var plan = SeedPlan.of(context);
    var tables = orderedTables();
    var counts = new LinkedHashMap<String, Integer>();
    var notes = new ArrayList<SeedNote>();
    try {
      dsl.transaction(
          cfg -> {
            var tx = DSL.using(cfg);
            requireEmpty(tx, tables);
            for (Table<?> table : tables) {
              counts.put(table.getName(), insert(tx, table, plan, context, notes));
            }
          });
    } catch (NotEmptyException e) {
      out.err("Cannot seed: tables are not empty (" + String.join(", ", e.relations()) + ").");
      return SeedResult.failure();
    } catch (DataAccessException e) {
      if (MISSING_TABLE.equals(e.sqlState())) {
        out.err("Cannot seed: a table is missing. Run `schema` first, or use `up`.");
      } else {
        out.err("Database error: " + e.getMessage());
      }
      return SeedResult.failure();
    }
    return new SeedResult(OK, counts, notes);
  }

  private static void requireEmpty(DSLContext dsl, List<Table<?>> tables) {
    var nonEmpty = new ArrayList<String>();
    for (Table<?> table : tables) {
      if (dsl.fetchExists(table)) {
        nonEmpty.add(table.getName());
      }
    }
    if (!nonEmpty.isEmpty()) {
      throw new NotEmptyException(nonEmpty);
    }
  }

  private int insert(
      DSLContext dsl, Table<?> table, SeedPlan plan, SeedContext context, List<SeedNote> notes) {
    TableSeeder seeder = seederFor(table);
    var fill = context.stream(table.getName() + ".fill");
    var rows = seeder.rows(plan, context, spec, notes);
    var batch = new ArrayList<TableRecord<?>>(BATCH_SIZE);
    int row = 0;
    while (rows.hasNext()) {
      TableRecord<?> record = rows.next();
      fillUnknown(record, fill, row++);
      batch.add(record);
      if (batch.size() == BATCH_SIZE) {
        dsl.batchInsert(batch).execute();
        batch.clear();
      }
    }
    if (!batch.isEmpty()) {
      dsl.batchInsert(batch).execute();
    }
    return row;
  }

  private static void fillUnknown(TableRecord<?> record, Random random, int row) {
    GenericValues.fillUnset(record, random, row);
  }

  private static TableSeeder seederFor(Table<?> table) {
    return switch (table.getName()) {
      case "lims" -> new LimsSeeder();
      case "workflow" -> new WorkflowSeeder();
      case "fastq" -> new FastqSeeder();
      case "bam" -> new BamSeeder();
      case "vcf" -> new VcfSeeder();
      case "workflow_output" -> new WorkflowOutputSeeder();
      case "workflow_cost_report" -> new WorkflowCostReportSeeder();
      default -> new GenericSeeder(table);
    };
  }

  private static List<Table<?>> orderedTables() {
    var tables = new ArrayList<Table<?>>(Mart.MART.getTables());
    // Stable sort: known tables by ORDER, unknown ones keep model order at the end.
    tables.sort(
        Comparator.comparingInt(
            t -> ORDER.contains(t.getName()) ? ORDER.indexOf(t.getName()) : ORDER.size()));
    return Collections.unmodifiableList(tables);
  }

  private static void printSummary(SeedResult result, Output out) {
    result.rowCounts().forEach((table, n) -> out.out("seed: " + table + ": " + n + " rows"));
    out.out("seed: total: " + result.totalRows() + " rows");
    for (SeedNote note : result.notes()) {
      switch (note.kind()) {
        case NO_SPEC -> out.out("warning: table " + note.table() + " has no relationship spec");
        case BOUNDARY_SKIPPED -> out.out("skipped boundary: " + note.table() + "." + note.column());
      }
    }
  }

  private static final class NotEmptyException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final transient List<String> relations;

    NotEmptyException(List<String> relations) {
      super("tables are not empty");
      this.relations = List.copyOf(relations);
    }

    List<String> relations() {
      return relations;
    }
  }
}
