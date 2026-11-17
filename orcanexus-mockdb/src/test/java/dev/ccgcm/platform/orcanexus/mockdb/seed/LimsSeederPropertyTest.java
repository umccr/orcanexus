package dev.ccgcm.platform.orcanexus.mockdb.seed;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccgcm.platform.orcanexus.jooq.tables.Lims;
import dev.ccgcm.platform.orcanexus.jooq.tables.records.LimsRecord;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jooq.Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Properties 3 and 7 of the offline mock database design, as parameterised JUnit tests over a seed
 * and scale grid (no property-based testing library in the stack).
 */
class LimsSeederPropertyTest {

  private static final int[] SEEDS = {0, 1, 42, 2024, Integer.MAX_VALUE};
  private static final int[] SCALES = {1, 2, 3, 4, 5, 9, 10, 11, 100, 1000};
  private static final Pattern LIBRARY_ID = Pattern.compile("L\\d{2}\\d{5}");
  private static final String TABLE = "lims";

  static Stream<Arguments> grid() {
    var args = new ArrayList<Arguments>();
    for (int seed : SEEDS) {
      for (int scale : SCALES) {
        args.add(Arguments.of(seed, scale));
      }
    }
    return args.stream();
  }

  private static List<LimsRecord> seed(int seed, int scale) {
    var ctx = new SeedContext(scale, seed);
    var rows = new ArrayList<LimsRecord>();
    new LimsSeeder()
        .rows(SeedPlan.of(ctx), ctx, RelationshipSpec.defaults(), new ArrayList<>())
        .forEachRemaining(rows::add);
    return rows;
  }

  private static List<Object> column(List<LimsRecord> rows, Field<?> field) {
    var values = new ArrayList<Object>(rows.size());
    for (var r : rows) {
      values.add(r.get(field));
    }
    return values;
  }

  // ---- Property 3 ------------------------------------------------------------------------

  /** Validates: Requirements 6.6, 6.7, 6.11 */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void libraryIdsAreDistinctAndWellFormed(int seed, int scale) {
    var rows = seed(seed, scale);
    var ids = column(rows, Lims.LIMS.LIBRARY_ID);
    assertThat(ids).hasSize(scale).doesNotContainNull();
    assertThat(new HashSet<>(ids)).hasSize(scale);
    assertThat(ids).allSatisfy(id -> assertThat((String) id).matches(LIBRARY_ID));
  }

  /** Validates: Requirements 6.6, 6.7, 6.11 */
  @Test
  void libraryIdsAreDistinctAtScale100000WithoutKeepingRecords() {
    int scale = 100_000;
    var ctx = new SeedContext(scale, 42);
    var it =
        new LimsSeeder()
            .rows(SeedPlan.of(ctx), ctx, RelationshipSpec.defaults(), new ArrayList<>());
    var seen = new HashSet<String>(scale * 2);
    int count = 0;
    while (it.hasNext()) {
      String id = it.next().get(Lims.LIMS.LIBRARY_ID);
      assertThat(id).isNotNull();
      assertThat(LIBRARY_ID.matcher(id).matches()).isTrue();
      seen.add(id);
      count++;
    }
    assertThat(count).isEqualTo(scale);
    assertThat(seen).hasSize(scale);
  }

  // ---- Property 7 ------------------------------------------------------------------------

  /** Validates: Requirements 8.3 */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void enoughRowsForTwoPages(int seed, int scale) {
    var rows = seed(seed, scale);
    assertThat(rows).hasSize(scale);
    if (scale >= 11) {
      assertThat(rows.size()).isGreaterThanOrEqualTo(11);
      assertThat((rows.size() + 9) / 10).isGreaterThanOrEqualTo(2);
    }
  }

  /** Validates: Requirements 8.4 */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void typeColumnHasTies(int seed, int scale) {
    if (scale < 5) {
      return; // no tie requirement below 5 rows
    }
    var counts = new TreeMap<String, Integer>();
    for (var r : seed(seed, scale)) {
      String type = r.get(Lims.LIMS.TYPE);
      if (type != null) {
        counts.merge(type, 1, Integer::sum);
      }
    }
    long repeated = counts.values().stream().filter(c -> c >= 2).count();
    assertThat(repeated).as("values appearing in >= 2 rows: %s", counts).isGreaterThanOrEqualTo(2);
  }

  /** Validates: Requirements 8.5 */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void ascendingAndDescendingSortsStartWithDifferentRows(int seed, int scale) {
    if (scale < 2) {
      return;
    }
    var rows = seed(seed, scale);
    // library_id is unique and non-null; sequencing_run_id is a pool column (never null) with
    // at least two runs once L >= 2.
    for (Field<String> f : List.of(Lims.LIMS.LIBRARY_ID, Lims.LIMS.SEQUENCING_RUN_ID)) {
      var sorted = new TreeMap<String, Integer>();
      for (var r : rows) {
        sorted.merge(r.get(f), 1, Integer::sum);
      }
      assertThat(sorted.firstKey()).as(f.getName()).isNotEqualTo(sorted.lastKey());
    }
  }

  // ---- null policy, pools, markers, determinism -------------------------------------------

  /** Validates: Requirements 8.3 (pool columns never null) */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void poolColumnsAreNeverNull(int seed, int scale) {
    var rows = seed(seed, scale);
    var spec = RelationshipSpec.defaults();
    int checked = 0;
    for (Field<?> f : Lims.LIMS.fields()) {
      if (spec.isPoolColumn(TABLE, f.getName())) {
        checked++;
        assertThat(column(rows, f)).as(f.getName()).doesNotContainNull();
      }
    }
    assertThat(checked).isGreaterThan(0);
  }

  /**
   * Validates: Requirements 8.5 (null policy). Columns whose rows are protected (type, dates) are
   * included: protected rows are only a handful, so the bounds still hold for the grid.
   */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void nonPoolNullableColumnsFollowNullPolicy(int seed, int scale) {
    var rows = seed(seed, scale);
    var spec = RelationshipSpec.defaults();
    int n = rows.size();
    for (Field<?> f : Lims.LIMS.fields()) {
      if (spec.isPoolColumn(TABLE, f.getName()) || !f.getDataType().nullable()) {
        continue;
      }
      long nulls = column(rows, f).stream().filter(v -> v == null).count();
      if (n >= 10) {
        assertThat(nulls * 10).as("%s nulls of %d", f.getName(), n).isGreaterThanOrEqualTo(n);
        assertThat(nulls * 2).as("%s nulls of %d", f.getName(), n).isLessThanOrEqualTo(n);
      } else {
        assertThat(nulls).as("%s nulls of %d", f.getName(), n).isGreaterThanOrEqualTo(1);
      }
    }
  }

  /** Validates: Requirements 8.3 (synthetic markers) */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void syntheticIdentifiersCarryMockMarker(int seed, int scale) {
    var rows = seed(seed, scale);
    for (Field<String> f :
        List.of(
            Lims.LIMS.PROJECT_ID,
            Lims.LIMS.OWNER_ID,
            Lims.LIMS.EXTERNAL_SUBJECT_ID,
            Lims.LIMS.EXTERNAL_SAMPLE_ID)) {
      for (var r : rows) {
        String v = r.get(f);
        if (v != null) {
          assertThat(v).as(f.getName()).startsWith("MOCK");
        }
      }
    }
  }

  /** Validates: Requirements 6.6 (determinism) */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void sameSeedAndScaleGiveIdenticalRows(int seed, int scale) {
    var a = seed(seed, scale);
    var b = seed(seed, scale);
    assertThat(a).hasSameSizeAs(b);
    for (int i = 0; i < a.size(); i++) {
      assertThat(a.get(i).intoMap()).as("row %d", i).isEqualTo(b.get(i).intoMap());
    }
  }

  /** Validates: Requirements 6.6 (different seeds differ) */
  @ParameterizedTest(name = "scale={0}")
  @MethodSource("largeScales")
  void differentSeedsDifferSomewhere(int scale) {
    Map<Integer, List<Map<String, Object>>> bySeed = new TreeMap<>();
    for (int seed : SEEDS) {
      bySeed.put(seed, seed(seed, scale).stream().map(LimsRecord::intoMap).toList());
    }
    var first = bySeed.get(SEEDS[0]);
    for (int seed : SEEDS) {
      if (seed != SEEDS[0]) {
        assertThat(bySeed.get(seed)).as("seed %d vs %d", seed, SEEDS[0]).isNotEqualTo(first);
      }
    }
  }

  static Stream<Integer> largeScales() {
    return Stream.of(5, 9, 10, 11, 100, 1000);
  }
}
