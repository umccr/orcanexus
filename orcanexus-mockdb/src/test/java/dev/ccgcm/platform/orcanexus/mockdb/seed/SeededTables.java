package dev.ccgcm.platform.orcanexus.mockdb.seed;

import dev.ccgcm.platform.orcanexus.jooq.Mart;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jooq.TableRecord;

/**
 * Test helper: seeds all seven tables in memory (detached jOOQ records), in {@link SeedRunner}'s
 * table order and with {@link GenericValues#fillUnset} applied exactly as the runner does. Results
 * are cached per seed and scale because the grid tests share them.
 */
final class SeededTables {

  /** Insertion order of {@link SeedRunner}. */
  static final List<String> ORDER =
      List.of("lims", "workflow", "fastq", "bam", "vcf", "workflow_output", "workflow_cost_report");

  private static final Map<String, SeededTables> CACHE = new ConcurrentHashMap<>();

  private final int scale;
  private final int seed;
  private final Map<String, List<TableRecord<?>>> rows;
  private final List<SeedNote> notes;
  private final KeyPools pools;

  private SeededTables(
      int scale,
      int seed,
      Map<String, List<TableRecord<?>>> rows,
      List<SeedNote> notes,
      KeyPools pools) {
    this.scale = scale;
    this.seed = seed;
    this.rows = rows;
    this.notes = notes;
    this.pools = pools;
  }

  static SeededTables of(int seed, int scale) {
    return CACHE.computeIfAbsent(seed + ":" + scale, k -> generate(seed, scale));
  }

  private static SeededTables generate(int seed, int scale) {
    var context = new SeedContext(scale, seed);
    var plan = SeedPlan.of(context);
    var spec = RelationshipSpec.defaults();
    var notes = new ArrayList<SeedNote>();
    var rows = new LinkedHashMap<String, List<TableRecord<?>>>();
    for (String name : ORDER) {
      var table = Mart.MART.getTable(name);
      var fill = context.stream(name + ".fill");
      var list = new ArrayList<TableRecord<?>>();
      var it = seederFor(name).rows(plan, context, spec, notes);
      int row = 0;
      while (it.hasNext()) {
        TableRecord<?> record = it.next();
        GenericValues.fillUnset(record, fill, row++);
        list.add(record);
      }
      if (table == null) {
        throw new IllegalStateException("missing table " + name);
      }
      rows.put(name, List.copyOf(list));
    }
    return new SeededTables(scale, seed, rows, List.copyOf(notes), plan.pools());
  }

  private static TableSeeder seederFor(String name) {
    return switch (name) {
      case "lims" -> new LimsSeeder();
      case "workflow" -> new WorkflowSeeder();
      case "fastq" -> new FastqSeeder();
      case "bam" -> new BamSeeder();
      case "vcf" -> new VcfSeeder();
      case "workflow_output" -> new WorkflowOutputSeeder();
      case "workflow_cost_report" -> new WorkflowCostReportSeeder();
      default -> throw new IllegalArgumentException(name);
    };
  }

  int scale() {
    return scale;
  }

  int seed() {
    return seed;
  }

  Map<String, List<TableRecord<?>>> rows() {
    return rows;
  }

  List<TableRecord<?>> rows(String table) {
    return rows.get(table);
  }

  List<SeedNote> notes() {
    return notes;
  }

  KeyPools pools() {
    return pools;
  }

  /** Values of one column in row order (nulls included). */
  List<Object> column(String table, String column) {
    var values = new ArrayList<Object>();
    for (TableRecord<?> r : rows.get(table)) {
      values.add(r.get(column));
    }
    return values;
  }

  /** All values as plain maps, per table, for equality checks. */
  Map<String, List<Map<String, Object>>> asMaps() {
    var out = new LinkedHashMap<String, List<Map<String, Object>>>();
    rows.forEach((t, list) -> out.put(t, list.stream().map(TableRecord::intoMap).toList()));
    return out;
  }
}
