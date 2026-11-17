package dev.ccgcm.platform.orcanexus.mockdb.seed;

import dev.ccgcm.platform.orcanexus.jooq.tables.Vcf;
import dev.ccgcm.platform.orcanexus.jooq.tables.records.VcfRecord;
import java.util.Iterator;
import java.util.List;
import org.jooq.TableRecord;

/**
 * Seeds {@code vcf}: {@code max(L, 4)} rows; row {@code j} takes workflow {@code j mod W}. See
 * {@link ArtifactColumns} for columns, null policy and boundary rows.
 */
public final class VcfSeeder implements TableSeeder {

  private static final String TABLE = "vcf";

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
    var t = Vcf.VCF;
    return new ArtifactColumns<VcfRecord>(
            t,
            VcfRecord::new,
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
            null,
            null,
            "VCF",
            (library, row) -> library + ".vcf.gz")
        .rows(plan, context, spec, notes);
  }
}
