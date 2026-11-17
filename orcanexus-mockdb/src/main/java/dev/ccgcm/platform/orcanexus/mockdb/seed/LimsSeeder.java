package dev.ccgcm.platform.orcanexus.mockdb.seed;

import dev.ccgcm.platform.orcanexus.jooq.tables.Lims;
import dev.ccgcm.platform.orcanexus.jooq.tables.records.LimsRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import org.jooq.Field;

/**
 * Seeds {@code lims}: one row per library.
 *
 * <p>Pool columns come from the {@link SeedPlan} and are never null. Free columns follow the null
 * policy. {@code sequencing_run_date} is the library's run date and {@code load_datetime} is that
 * date at midnight plus {@link #LOAD_OFFSET_SECONDS}. When boundary rows fit (L >= 3), library 0
 * holds the earliest values and the first library of the last run holds the latest values of both
 * date columns ({@link Boundaries#timestampMin()} and {@link Boundaries#timestampMax()} for {@code
 * load_datetime}); both rows are protected from nulls. Otherwise a {@link
 * SeedNote.Kind#BOUNDARY_SKIPPED} note names {@code lims.<column>}. For L >= 5 the column {@code
 * type} carries tie rows: vocabulary size {@code min(3, L / 2)}, rows 0 and 1 hold the first value,
 * rows 2 and 3 the second, other rows {@code vocab[i mod k]}; rows 0 to 3 are protected from nulls.
 */
public final class LimsSeeder implements TableSeeder {

  /** Seconds added to the run date to get {@code load_datetime}. */
  static final int LOAD_OFFSET_SECONDS = 21_600;

  private static final String TABLE = "lims";
  private static final int TIE_MIN_ROWS = 5;
  private static final int TIE_ROWS = 4;

  private static final String[] TYPES = {"WGS", "WTS", "ctDNA"};
  private static final String[] ASSAYS = {"TsqNano", "TsqStranded", "10X"};
  private static final String[] PHENOTYPES = {"tumor", "normal", "negative-control"};
  private static final String[] QUALITIES = {"good", "ok", "poor"};
  private static final String[] SOURCES = {"blood", "tissue", "FFPE"};
  private static final String[] WORKFLOWS = {"clinical", "research", "qc"};
  private static final String BASES = "ACGT";

  @Override
  public String table() {
    return TABLE;
  }

  @Override
  public int rowCount(SeedPlan plan) {
    return plan.rowCount(TABLE);
  }

  @Override
  public Iterator<LimsRecord> rows(
      SeedPlan plan, SeedContext context, RelationshipSpec spec, List<SeedNote> notes) {
    final int n = rowCount(plan);
    final boolean ties = n >= TIE_MIN_ROWS;
    final int vocab = Math.min(TYPES.length, n / 2);

    var boundary = Boundaries.place(n, true);
    var boundaryRows = new BitSet(n);
    int lastRunFirstLibrary = plan.runCount() - 1;
    if (boundary.fits()) {
      boundaryRows.set(0);
      boundaryRows.set(lastRunFirstLibrary);
    } else {
      notes.add(skipped(Lims.LIMS.SEQUENCING_RUN_DATE));
      notes.add(skipped(Lims.LIMS.LOAD_DATETIME));
    }
    var tieRows = new BitSet(n);
    if (ties) {
      tieRows.set(0, TIE_ROWS);
    }

    Random nullRandom = context.stream("lims.nulls");
    var nulls = new LinkedHashMap<Field<?>, NullPlan>();
    for (Field<?> field : Lims.LIMS.fields()) {
      BitSet protectedRows = null;
      if (field.equals(Lims.LIMS.TYPE)) {
        protectedRows = tieRows;
      } else if (field.equals(Lims.LIMS.SEQUENCING_RUN_DATE)
          || field.equals(Lims.LIMS.LOAD_DATETIME)) {
        protectedRows = boundaryRows;
      }
      nulls.put(field, NullPolicy.planFor(TABLE, field, spec, n, protectedRows, nullRandom));
    }

    Random random = context.stream("lims.values");
    final boolean boundaryFits = boundary.fits();

    return new Iterator<>() {
      private int row;

      @Override
      public boolean hasNext() {
        return row < n;
      }

      @Override
      public LimsRecord next() {
        if (row >= n) {
          throw new NoSuchElementException();
        }
        int i = row++;
        var r = new LimsRecord();
        LocalDate date = plan.libraryDate(i);
        LocalDateTime load = date.atStartOfDay().plusSeconds(LOAD_OFFSET_SECONDS);
        if (boundaryFits && i == 0) {
          date = Boundaries.dateMin();
          load = Boundaries.timestampMin();
        } else if (boundaryFits && i == lastRunFirstLibrary) {
          date = Boundaries.dateMax();
          load = Boundaries.timestampMax();
        }

        // Draw in a fixed order so the stream does not depend on which values end up null.
        String alias = String.format("MOCK-ALIAS-%05d", i);
        String experiment = String.format("MOCK-EXP-%05d", random.nextInt(100_000));
        String project = String.format("MOCK-PROJ-%02d", random.nextInt(20));
        String owner = String.format("MOCK-OWNER-%02d", random.nextInt(10));
        String workflow = WORKFLOWS[random.nextInt(WORKFLOWS.length)];
        String phenotype = PHENOTYPES[random.nextInt(PHENOTYPES.length)];
        String assay = ASSAYS[random.nextInt(ASSAYS.length)];
        String quality = QUALITIES[random.nextInt(QUALITIES.length)];
        String source = SOURCES[random.nextInt(SOURCES.length)];
        String truseq = truseqIndex(random);
        String type = typeFor(i, ties, vocab, random);

        // Pool columns: never null.
        r.set(Lims.LIMS.SEQUENCING_RUN_ID, plan.libraryRunId(i));
        r.set(Lims.LIMS.LIBRARY_ID, plan.libraryId(i));
        r.set(Lims.LIMS.INTERNAL_SUBJECT_ID, plan.librarySubjectId(i));
        r.set(Lims.LIMS.SAMPLE_ID, plan.librarySampleId(i));
        // Free columns.
        put(r, nulls, i, Lims.LIMS.SEQUENCING_RUN_DATE, date);
        put(r, nulls, i, Lims.LIMS.ALIAS_LIBRARY_ID, alias);
        put(
            r,
            nulls,
            i,
            Lims.LIMS.EXTERNAL_SUBJECT_ID,
            String.format("MOCK-EXTSBJ-%05d", plan.librarySubjectIndex(i)));
        put(
            r,
            nulls,
            i,
            Lims.LIMS.EXTERNAL_SAMPLE_ID,
            String.format("MOCK-EXTSMP-%05d", plan.librarySampleIndex(i)));
        put(r, nulls, i, Lims.LIMS.EXPERIMENT_ID, experiment);
        put(r, nulls, i, Lims.LIMS.PROJECT_ID, project);
        put(r, nulls, i, Lims.LIMS.OWNER_ID, owner);
        put(r, nulls, i, Lims.LIMS.WORKFLOW, workflow);
        put(r, nulls, i, Lims.LIMS.PHENOTYPE, phenotype);
        put(r, nulls, i, Lims.LIMS.TYPE, type);
        put(r, nulls, i, Lims.LIMS.ASSAY, assay);
        put(r, nulls, i, Lims.LIMS.QUALITY, quality);
        put(r, nulls, i, Lims.LIMS.SOURCE, source);
        put(r, nulls, i, Lims.LIMS.TRUSEQ_INDEX, truseq);
        put(r, nulls, i, Lims.LIMS.LOAD_DATETIME, load);
        return r;
      }
    };
  }

  private static <T> void put(
      LimsRecord record, Map<Field<?>, NullPlan> nulls, int row, Field<T> field, T value) {
    record.set(field, nulls.get(field).isNull(row) ? null : value);
  }

  private static String typeFor(int i, boolean ties, int vocab, Random random) {
    int draw = random.nextInt(TYPES.length);
    if (ties) {
      return TYPES[i < TIE_ROWS ? i / 2 : i % vocab];
    }
    return TYPES[draw];
  }

  private static String truseqIndex(Random random) {
    var sb = new StringBuilder(8);
    for (int k = 0; k < 8; k++) {
      sb.append(BASES.charAt(random.nextInt(BASES.length())));
    }
    return sb.toString();
  }

  private static SeedNote skipped(Field<?> field) {
    String name = TABLE + "." + field.getName();
    return new SeedNote(
        SeedNote.Kind.BOUNDARY_SKIPPED, TABLE, field.getName(), name + ": too few rows");
  }
}
