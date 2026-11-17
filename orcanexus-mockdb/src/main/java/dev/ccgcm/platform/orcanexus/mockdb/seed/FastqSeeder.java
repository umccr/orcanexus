package dev.ccgcm.platform.orcanexus.mockdb.seed;

import dev.ccgcm.platform.orcanexus.jooq.tables.Fastq;
import dev.ccgcm.platform.orcanexus.jooq.tables.records.FastqRecord;
import java.util.Iterator;
import java.util.List;
import org.jooq.TableRecord;

/**
 * Seeds {@code fastq}: {@code W} rows, one per workflow, alternating R1 and R2 file names. {@code
 * sequencing_run_id} and {@code sequencing_run_date} come from the library's plan so they agree
 * with {@code lims}; see {@link ArtifactColumns} for the boundary rows (on rows 0 and 1 the date is
 * overridden with the range minimum and maximum, the run id is kept).
 */
public final class FastqSeeder implements TableSeeder {

  private static final String TABLE = "fastq";

  @Override
  public String table() {
    return TABLE;
  }

  @Override
  public int rowCount(SeedPlan plan) {
    return plan.rowCount(TABLE);
  }

  @Override
  public Iterator<? extends TableRecord<?>> rows(
      SeedPlan plan, SeedContext context, RelationshipSpec spec, List<SeedNote> notes) {
    var t = Fastq.FASTQ;
    return new ArtifactColumns<FastqRecord>(
            t,
            FastqRecord::new,
            t.PORTAL_RUN_ID,
            t.COHORT_ID,
            t.BUCKET,
            t.KEY,
            t.LIBRARY_ID,
            t.FILENAME,
            t.FORMAT,
            t.SIZE,
            t.STORAGE_CLASS,
            t.E_TAG,
            t.LAST_MODIFIED_DATE,
            t.SEQUENCING_RUN_ID,
            t.SEQUENCING_RUN_DATE,
            "FASTQ",
            (library, row) -> library + (row % 2 == 0 ? "_R1" : "_R2") + ".fastq.gz")
        .rows(plan, context, spec, notes);
  }
}
