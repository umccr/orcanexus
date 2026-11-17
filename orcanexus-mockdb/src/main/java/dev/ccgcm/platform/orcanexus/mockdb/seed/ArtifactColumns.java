package dev.ccgcm.platform.orcanexus.mockdb.seed;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.Supplier;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableRecord;

/**
 * Shared row generation for the artifact tables {@code fastq}, {@code bam} and {@code vcf}, which
 * have the same columns (fastq adds the sequencing run columns).
 *
 * <p>Row {@code j} takes workflow {@code j mod W} of the plan: {@code portal_run_id} and {@code
 * library_id} come from it, so every child key exists in {@code workflow} and {@code lims}. {@code
 * cohort_id} is chosen by library index modulo the cohort pool size and {@code bucket} by row
 * modulo the bucket pool size; both are pool columns and never null. All other columns are free and
 * subject to the null policy.
 *
 * <p>Boundary rows: row 0 holds the minimum and row 1 the maximum for {@code size} (0 and {@code
 * Long.MAX_VALUE}), {@code last_modified_date} ({@link Boundaries#timestampMin()} and {@link
 * Boundaries#timestampMax()}) and, for fastq, {@code sequencing_run_date} ({@link
 * Boundaries#dateMin()} and {@link Boundaries#dateMax()}). Those rows are protected from nulls for
 * those columns. On the fastq boundary rows only the free date column is overridden; {@code
 * sequencing_run_id} stays the library's run id, so the date on those two rows may differ from the
 * date of the run in {@code lims}.
 */
final class ArtifactColumns<R extends TableRecord<R>> {

  private static final List<String> STORAGE_CLASSES =
      List.of("STANDARD", "GLACIER", "INTELLIGENT_TIERING");
  private static final long MIN_SIZE = 1_000L;
  private static final long MAX_RANDOM_SIZE = 500_000_000_000L;

  /** How the file name of a row is built. */
  @FunctionalInterface
  interface FileNamer {
    String name(String library, int row);
  }

  private final Table<R> table;
  private final Supplier<R> recordFactory;
  private final TableField<R, String> portalRunId;
  private final TableField<R, String> cohortId;
  private final TableField<R, String> bucket;
  private final TableField<R, String> key;
  private final TableField<R, String> libraryId;
  private final TableField<R, String> filename;
  private final TableField<R, String> format;
  private final TableField<R, Long> size;
  private final TableField<R, String> storageClass;
  private final TableField<R, String> eTag;
  private final TableField<R, LocalDateTime> lastModified;
  // Fastq only (null for bam and vcf).
  private final TableField<R, String> runId;
  private final TableField<R, LocalDate> runDate;
  private final String formatValue;
  private final FileNamer namer;

  @SuppressWarnings("java:S107")
  ArtifactColumns(
      Table<R> table,
      Supplier<R> recordFactory,
      TableField<R, String> portalRunId,
      TableField<R, String> cohortId,
      TableField<R, String> bucket,
      TableField<R, String> key,
      TableField<R, String> libraryId,
      TableField<R, String> filename,
      TableField<R, String> format,
      TableField<R, Long> size,
      TableField<R, String> storageClass,
      TableField<R, String> eTag,
      TableField<R, LocalDateTime> lastModified,
      TableField<R, String> runId,
      TableField<R, LocalDate> runDate,
      String formatValue,
      FileNamer namer) {
    this.table = table;
    this.recordFactory = recordFactory;
    this.portalRunId = portalRunId;
    this.cohortId = cohortId;
    this.bucket = bucket;
    this.key = key;
    this.libraryId = libraryId;
    this.filename = filename;
    this.format = format;
    this.size = size;
    this.storageClass = storageClass;
    this.eTag = eTag;
    this.lastModified = lastModified;
    this.runId = runId;
    this.runDate = runDate;
    this.formatValue = formatValue;
    this.namer = namer;
  }

  Iterator<R> rows(
      SeedPlan plan, SeedContext context, RelationshipSpec spec, List<SeedNote> notes) {
    final String name = table.getName();
    final int n = plan.rowCount(name);
    final int workflows = plan.workflowCount();
    final var pools = plan.pools();
    final int cohorts = pools.size(Pool.COHORT);
    final int buckets = pools.size(Pool.BUCKET);

    var placement = Boundaries.place(n, true);
    final boolean fits = placement.fits();
    var boundaryRows = new BitSet(n);
    if (fits) {
      boundaryRows.set(placement.minRow());
      boundaryRows.set(placement.maxRow());
    } else {
      for (Field<?> f : boundaryFields()) {
        notes.add(
            new SeedNote(
                SeedNote.Kind.BOUNDARY_SKIPPED,
                name,
                f.getName(),
                name + "." + f.getName() + ": too few rows"));
      }
    }

    Random nullRandom = context.stream(name + ".nulls");
    var nulls = new LinkedHashMap<Field<?>, NullPlan>();
    for (Field<?> field : table.fields()) {
      BitSet protectedRows = boundaryFields().contains(field) ? boundaryRows : null;
      nulls.put(field, NullPolicy.planFor(name, field, spec, n, protectedRows, nullRandom));
    }

    final Random values = context.stream(name + ".values");
    final var faker = context.faker(values);
    final long firstDay = SeedPlan.RANGE_START.toEpochDay();
    final long daySpan = SeedPlan.RANGE_END.toEpochDay() - firstDay + 1;

    return new Iterator<>() {
      private int row;

      @Override
      public boolean hasNext() {
        return row < n;
      }

      @Override
      public R next() {
        if (row >= n) {
          throw new NoSuchElementException();
        }
        int j = row++;
        int w = j % workflows;
        int libraryIndex = plan.workflowLibraryIndex(w);
        String library = plan.workflowLibraryId(w);
        String cohort = pools.id(Pool.COHORT, libraryIndex % cohorts);
        String bucketId = pools.id(Pool.BUCKET, j % buckets);
        String file = namer.name(library, j);

        // Always draw, so the stream does not depend on which values end up null.
        long sizeValue = MIN_SIZE + (long) (values.nextDouble() * (MAX_RANDOM_SIZE - MIN_SIZE));
        String storage = STORAGE_CLASSES.get(values.nextInt(STORAGE_CLASSES.size()));
        String etag = faker.regexify("[a-f0-9]{32}");
        LocalDateTime modified =
            LocalDateTime.of(
                LocalDate.ofEpochDay(firstDay + values.nextInt((int) daySpan)),
                java.time.LocalTime.ofSecondOfDay(values.nextInt(86_400)));

        LocalDate date = runDate == null ? null : plan.libraryDate(libraryIndex);
        if (fits && j == placement.minRow()) {
          sizeValue = Boundaries.bigintMin();
          modified = Boundaries.timestampMin();
          date = runDate == null ? null : Boundaries.dateMin();
        } else if (fits && j == placement.maxRow()) {
          sizeValue = Boundaries.bigintMax();
          modified = Boundaries.timestampMax();
          date = runDate == null ? null : Boundaries.dateMax();
        }

        R r = recordFactory.get();
        // Pool columns: never null.
        r.set(portalRunId, plan.workflowPortalRunId(w));
        r.set(libraryId, library);
        r.set(cohortId, cohort);
        r.set(bucket, bucketId);
        if (runId != null) {
          r.set(runId, plan.libraryRunId(libraryIndex));
        }
        // Free columns.
        put(r, nulls, j, key, "mock/" + cohort + "/" + library + "/" + file);
        put(r, nulls, j, filename, file);
        put(r, nulls, j, format, formatValue);
        put(r, nulls, j, size, sizeValue);
        put(r, nulls, j, storageClass, storage);
        put(r, nulls, j, eTag, etag);
        put(r, nulls, j, lastModified, modified);
        if (runDate != null) {
          put(r, nulls, j, runDate, date);
        }
        return r;
      }
    };
  }

  private List<Field<?>> boundaryFields() {
    return runDate == null ? List.of(size, lastModified) : List.of(size, lastModified, runDate);
  }

  private static <T, R extends TableRecord<R>> void put(
      R record, Map<Field<?>, NullPlan> nulls, int row, TableField<R, T> field, T value) {
    record.set(field, nulls.get(field).isNull(row) ? null : value);
  }
}
