package dev.ccgcm.platform.orcanexus.mockdb.seed;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccgcm.platform.orcanexus.jooq.Mart;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jooq.Field;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Properties 1, 2, 4, 5, 6 and 8 of the offline mock database design for all seven tables, as
 * parameterised JUnit tests over a seed and scale grid (no property-based testing library in the
 * stack). Lims-specific properties live in {@link LimsSeederPropertyTest}.
 */
class SeedPropertiesTest {

  private static final int[] SEEDS = {0, 1, 42, 2024, Integer.MAX_VALUE};
  private static final int[] SCALES = {1, 2, 3, 4, 5, 9, 10, 11, 100, 1000};
  private static final RelationshipSpec SPEC = RelationshipSpec.defaults();
  private static final LocalDate DATE_MIN = LocalDate.of(2022, 1, 1);
  private static final LocalDate DATE_MAX = LocalDate.of(2025, 12, 31);
  private static final LocalDateTime TS_MIN = DATE_MIN.atStartOfDay();
  private static final LocalDateTime TS_MAX = DATE_MAX.atTime(LocalTime.of(23, 59, 59));
  private static final Pattern PHONE = Pattern.compile("\\+?\\d{2,3}[-. ]\\d{3,4}[-. ]\\d{3,4}");

  static Stream<Arguments> grid() {
    var args = new ArrayList<Arguments>();
    for (int seed : SEEDS) {
      for (int scale : SCALES) {
        args.add(Arguments.of(seed, scale));
      }
    }
    return args.stream();
  }

  static Stream<Arguments> scalesAtLeastFive() {
    return Stream.of(5, 9, 10, 11, 100, 1000).map(Arguments::of);
  }

  static Stream<Arguments> scales() {
    return java.util.Arrays.stream(SCALES).mapToObj(Arguments::of);
  }

  private static int expectedRows(String table, int scale) {
    return switch (table) {
      case "lims" -> scale;
      case "workflow", "fastq", "workflow_output", "workflow_cost_report" -> Math.max(2 * scale, 4);
      case "bam", "vcf" -> Math.max(scale, 4);
      default -> throw new IllegalArgumentException(table);
    };
  }

  private static List<Field<?>> fields(String table) {
    return List.of(Mart.MART.getTable(table).fields());
  }

  private static Set<Object> nonNull(SeededTables data, String table, String column) {
    var set = new HashSet<Object>(data.column(table, column));
    set.remove(null);
    return set;
  }

  // ---- Property 1: determinism --------------------------------------------------------------

  /** Validates: Requirements 6.3, 6.4, 13.5 */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void determinismSameSeedAndScaleGiveIdenticalRowsForAllTables(int seed, int scale) {
    var a = SeededTables.of(seed, scale);
    // Bypass the cache: generate again from scratch.
    var b = fresh(seed, scale);
    assertThat(b.asMaps()).isEqualTo(a.asMaps());
    assertThat(b.notes()).isEqualTo(a.notes());
  }

  private static SeededTables fresh(int seed, int scale) {
    try {
      var m = SeededTables.class.getDeclaredMethod("generate", int.class, int.class);
      m.setAccessible(true);
      return (SeededTables) m.invoke(null, seed, scale);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Validates: Requirements 6.3, 13.5 */
  @ParameterizedTest(name = "scale={0}")
  @MethodSource("scalesAtLeastFive")
  void determinismDifferentSeedsDifferInAtLeastOneValue(int scale) {
    var base = SeededTables.of(SEEDS[0], scale).asMaps();
    for (int i = 1; i < SEEDS.length; i++) {
      assertThat(SeededTables.of(SEEDS[i], scale).asMaps())
          .as("seed %d vs %d", SEEDS[i], SEEDS[0])
          .isNotEqualTo(base);
    }
  }

  // ---- Property 2: row counts ---------------------------------------------------------------

  /** Validates: Requirements 6.1, 6.5 */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void rowCountsDependOnlyOnScale(int seed, int scale) {
    var data = SeededTables.of(seed, scale);
    assertThat(data.rows().keySet()).containsExactlyElementsOf(SeededTables.ORDER);
    for (String table : SeededTables.ORDER) {
      assertThat(data.rows(table)).as(table).hasSize(expectedRows(table, scale));
      assertThat(data.rows(table)).as(table).isNotEmpty();
    }
  }

  // ---- Property 4: pools and referential consistency -----------------------------------------

  /** Validates: Requirements 7.1 */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void poolColumnsAreNonNullAndMembersOfTheirPool(int seed, int scale) {
    var data = SeededTables.of(seed, scale);
    for (String table : SeededTables.ORDER) {
      for (var entry : SPEC.columnsOf(table).entrySet()) {
        var members = new HashSet<Object>(data.pools().ids(entry.getValue()));
        assertThat(data.column(table, entry.getKey()))
            .as("%s.%s", table, entry.getKey())
            .doesNotContainNull()
            .allSatisfy(v -> assertThat(members).contains(v));
      }
    }
  }

  /** Validates: Requirements 7.3, 7.5 */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void libraryIdsExistInLimsAndEachJoinReturnsRows(int seed, int scale) {
    var data = SeededTables.of(seed, scale);
    var lims = nonNull(data, "lims", "library_id");
    for (String child : List.of("fastq", "bam", "vcf", "workflow")) {
      var ids = nonNull(data, child, "library_id");
      assertThat(lims).as("lims contains %s.library_id", child).containsAll(ids);
      assertThat(ids).as("join lims-%s", child).isNotEmpty();
      assertThat(new HashSet<>(ids)).as("join lims-%s", child).anyMatch(lims::contains);
    }
  }

  /** Validates: Requirements 7.4, 7.6 */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void portalRunIdsExistInWorkflowAndEachJoinReturnsRows(int seed, int scale) {
    var data = SeededTables.of(seed, scale);
    var workflow = nonNull(data, "workflow", "portal_run_id");
    for (String child : List.of("fastq", "bam", "vcf", "workflow_output", "workflow_cost_report")) {
      var ids = nonNull(data, child, "portal_run_id");
      assertThat(workflow).as("workflow contains %s.portal_run_id", child).containsAll(ids);
      assertThat(ids).as("join workflow-%s", child).isNotEmpty();
    }
  }

  // ---- Property 5: null policy --------------------------------------------------------------

  /** Validates: Requirements 8.1, 8.2 */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void nullPolicyHoldsForEveryTable(int seed, int scale) {
    var data = SeededTables.of(seed, scale);
    for (String table : SeededTables.ORDER) {
      int n = data.rows(table).size();
      for (Field<?> f : fields(table)) {
        String name = table + "." + f.getName();
        long nulls = data.column(table, f.getName()).stream().filter(v -> v == null).count();
        if (SPEC.isPoolColumn(table, f.getName()) || !f.getDataType().nullable()) {
          assertThat(nulls).as(name).isZero();
        } else if (n >= 10) {
          assertThat(nulls * 10).as("%s: %d nulls of %d", name, nulls, n).isGreaterThanOrEqualTo(n);
          assertThat(nulls * 2).as("%s: %d nulls of %d", name, nulls, n).isLessThanOrEqualTo(n);
        } else {
          assertThat(nulls).as("%s: nulls of %d", name, n).isGreaterThanOrEqualTo(1);
        }
      }
    }
  }

  // ---- Property 6: boundary rows ------------------------------------------------------------

  /** Validates: Requirements 8.6, 8.7 */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void boundaryRowsExistOrAreReportedAsSkipped(int seed, int scale) {
    var data = SeededTables.of(seed, scale);
    var skipped = new HashSet<String>();
    for (SeedNote note : data.notes()) {
      if (note.kind() == SeedNote.Kind.BOUNDARY_SKIPPED) {
        skipped.add(note.table() + "." + note.column());
      }
    }
    // Only lims can be too small (L < 3); every other table has at least 4 rows.
    var expectedSkips = new HashSet<String>();
    if (scale < 3) {
      expectedSkips.add("lims.sequencing_run_date");
      expectedSkips.add("lims.load_datetime");
    }
    assertThat(skipped).isEqualTo(expectedSkips);

    for (String table : SeededTables.ORDER) {
      for (Field<?> f : fields(table)) {
        if (SPEC.isPoolColumn(table, f.getName())) {
          continue;
        }
        Object[] bounds = boundsOf(f);
        if (bounds == null) {
          continue;
        }
        String name = table + "." + f.getName();
        if (skipped.contains(name)) {
          continue;
        }
        var values = nonNull(data, table, f.getName());
        assertThat(contains(values, bounds[0])).as("%s min %s", name, bounds[0]).isTrue();
        assertThat(contains(values, bounds[1])).as("%s max %s", name, bounds[1]).isTrue();
      }
    }
  }

  /** Min and max expected for a boundary-relevant column type, or null for other types. */
  private static Object[] boundsOf(Field<?> f) {
    Class<?> type = f.getDataType().getType();
    if (type == Long.class) {
      return new Object[] {0L, Long.MAX_VALUE};
    }
    if (type == BigDecimal.class) {
      int p = f.getDataType().precision();
      int s = f.getDataType().scale();
      var max = new BigDecimal(BigInteger.TEN.pow(p).subtract(BigInteger.ONE), s);
      return new Object[] {BigDecimal.ZERO, max};
    }
    if (type == LocalDate.class) {
      return new Object[] {DATE_MIN, DATE_MAX};
    }
    if (type == LocalDateTime.class) {
      return new Object[] {TS_MIN, TS_MAX};
    }
    return null;
  }

  private static boolean contains(Set<Object> values, Object expected) {
    for (Object v : values) {
      if (v instanceof BigDecimal b
          ? b.compareTo((BigDecimal) expected) == 0
          : v.equals(expected)) {
        return true;
      }
    }
    return false;
  }

  // ---- Property 8: values fit the model -----------------------------------------------------

  /** Validates: Requirements 6.10 */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void generatedValuesFitTheModel(int seed, int scale) {
    var data = SeededTables.of(seed, scale);
    for (String table : SeededTables.ORDER) {
      for (Field<?> f : fields(table)) {
        String name = table + "." + f.getName();
        var type = f.getDataType();
        for (Object v : data.column(table, f.getName())) {
          if (v == null) {
            continue;
          }
          if (v instanceof String s && type.hasLength() && type.length() > 0) {
            assertThat(s.length()).as(name).isLessThanOrEqualTo(type.length());
          } else if (v instanceof BigDecimal b) {
            assertThat(b.scale()).as(name + " scale").isLessThanOrEqualTo(type.scale());
            assertThat(b.precision() - b.scale())
                .as(name + " integer digits of " + b)
                .isLessThanOrEqualTo(type.precision() - type.scale());
          } else if (v instanceof Long l) {
            assertThat(l).as(name).isBetween(0L, Long.MAX_VALUE);
          } else if (v instanceof LocalDate d) {
            assertThat(d).as(name).isBetween(DATE_MIN, DATE_MAX);
          } else if (v instanceof LocalDateTime t) {
            assertThat(t).as(name).isBetween(TS_MIN, TS_MAX);
            assertThat(t.getNano() % 1000).as(name + " sub-microsecond").isZero();
          }
        }
      }
    }
  }

  // ---- synthetic markers and no real personal data ------------------------------------------

  /** Validates: Requirements 6.12, 6.13 */
  @ParameterizedTest(name = "seed={0} scale={1}")
  @MethodSource("grid")
  void syntheticMarkersAndNoEmailOrPhoneLookalikes(int seed, int scale) {
    var data = SeededTables.of(seed, scale);
    for (String table : SeededTables.ORDER) {
      for (Field<?> f : fields(table)) {
        String name = f.getName();
        for (Object v : data.column(table, name)) {
          if (v instanceof String s) {
            assertThat(s).as("%s.%s", table, name).doesNotContain("@");
            assertThat(PHONE.matcher(s).find()).as("%s.%s: %s", table, name, s).isFalse();
            if (name.equals("bucket")
                || name.equals("ica_project")
                || name.equals("workflow_comment")) {
              assertThat(s).as("%s.%s", table, name).startsWithIgnoringCase("mock");
            } else if (name.equals("cohort_id")) {
              assertThat(s).as("%s.%s", table, name).containsIgnoringCase("mock");
            }
          }
        }
      }
    }
  }
}
