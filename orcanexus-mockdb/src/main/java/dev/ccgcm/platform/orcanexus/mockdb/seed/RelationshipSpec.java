package dev.ccgcm.platform.orcanexus.mockdb.seed;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jooq.Schema;
import org.jooq.Table;

/**
 * States which columns of which tables draw their values from which {@link Pool}. Columns not
 * listed are free columns. Immutable; iteration order is the declaration order.
 */
public final class RelationshipSpec {

  private final Map<String, Map<String, Pool>> entries;

  private RelationshipSpec(Map<String, Map<String, Pool>> entries) {
    var copy = new LinkedHashMap<String, Map<String, Pool>>();
    entries.forEach(
        (table, columns) ->
            copy.put(table, Collections.unmodifiableMap(new LinkedHashMap<>(columns))));
    this.entries = Collections.unmodifiableMap(copy);
  }

  /** Builds a spec from a table to column to pool map (copied defensively). */
  public static RelationshipSpec of(Map<String, Map<String, Pool>> entries) {
    return new RelationshipSpec(entries);
  }

  /** The spec for the known warehouse tables. */
  public static RelationshipSpec defaults() {
    var m = new LinkedHashMap<String, Map<String, Pool>>();
    m.put(
        "lims",
        columns(
            "sequencing_run_id", Pool.SEQUENCING_RUN,
            "library_id", Pool.LIBRARY,
            "internal_subject_id", Pool.SUBJECT,
            "sample_id", Pool.SAMPLE));
    m.put(
        "fastq",
        columns(
            "sequencing_run_id", Pool.SEQUENCING_RUN,
            "portal_run_id", Pool.PORTAL_RUN,
            "cohort_id", Pool.COHORT,
            "bucket", Pool.BUCKET,
            "library_id", Pool.LIBRARY));
    for (String table : List.of("bam", "vcf")) {
      m.put(
          table,
          columns(
              "portal_run_id", Pool.PORTAL_RUN,
              "cohort_id", Pool.COHORT,
              "bucket", Pool.BUCKET,
              "library_id", Pool.LIBRARY));
    }
    m.put("workflow", columns("portal_run_id", Pool.PORTAL_RUN, "library_id", Pool.LIBRARY));
    m.put(
        "workflow_output",
        columns("portal_run_id", Pool.PORTAL_RUN, "cohort_id", Pool.COHORT, "bucket", Pool.BUCKET));
    m.put("workflow_cost_report", columns("portal_run_id", Pool.PORTAL_RUN));
    return new RelationshipSpec(m);
  }

  private static Map<String, Pool> columns(Object... pairs) {
    var map = new LinkedHashMap<String, Pool>();
    for (int i = 0; i < pairs.length; i += 2) {
      map.put((String) pairs[i], (Pool) pairs[i + 1]);
    }
    return map;
  }

  /** The pool a column draws from, or empty for a free column or unknown table. */
  public Optional<Pool> poolOf(String table, String column) {
    return Optional.ofNullable(entries.get(table)).map(c -> c.get(column));
  }

  public boolean isPoolColumn(String table, String column) {
    return poolOf(table, column).isPresent();
  }

  public boolean hasEntry(String table) {
    return entries.containsKey(table);
  }

  /** Table names with an entry, in declaration order. */
  public List<String> tables() {
    return List.copyOf(entries.keySet());
  }

  /** Pool columns of a table in declaration order (empty when the table has no entry). */
  public Map<String, Pool> columnsOf(String table) {
    return entries.getOrDefault(table, Map.of());
  }

  /**
   * Checks that every named table and column exists in the schema.
   *
   * @throws SpecMismatchException naming the first missing table (column {@code null}) or column
   */
  public void validate(Schema schema) {
    for (var entry : entries.entrySet()) {
      Table<?> table = schema.getTable(entry.getKey());
      if (table == null) {
        throw new SpecMismatchException(entry.getKey(), null);
      }
      for (String column : entry.getValue().keySet()) {
        if (table.field(column) == null) {
          throw new SpecMismatchException(entry.getKey(), column);
        }
      }
    }
  }
}
