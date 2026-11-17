package dev.ccgcm.platform.orcanexus.mockdb.seed;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccgcm.platform.orcanexus.jooq.tables.Bam;
import dev.ccgcm.platform.orcanexus.jooq.tables.Fastq;
import dev.ccgcm.platform.orcanexus.jooq.tables.Vcf;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jooq.TableRecord;
import org.junit.jupiter.api.Test;

class ArtifactSeedersSmokeTest {

  private static List<TableRecord<?>> seed(TableSeeder seeder, SeedContext ctx, SeedPlan plan) {
    var rows = new ArrayList<TableRecord<?>>();
    seeder
        .rows(plan, ctx, RelationshipSpec.defaults(), new ArrayList<>())
        .forEachRemaining(rows::add);
    return rows;
  }

  @Test
  void keysExistInWorkflowAndLimsAndPoolColumnsAreSet() {
    var ctx = new SeedContext(100, 42);
    var plan = SeedPlan.of(ctx);
    Set<String> portal = new HashSet<>();
    Set<String> libs = new HashSet<>();
    for (int j = 0; j < plan.workflowCount(); j++) {
      portal.add(plan.workflowPortalRunId(j));
    }
    for (int i = 0; i < plan.libraryCount(); i++) {
      libs.add(plan.libraryId(i));
    }
    var fastq = seed(new FastqSeeder(), ctx, plan);
    var bam = seed(new BamSeeder(), ctx, plan);
    var vcf = seed(new VcfSeeder(), ctx, plan);
    assertThat(fastq).hasSize(200);
    assertThat(bam).hasSize(100);
    assertThat(vcf).hasSize(100);
    for (var rows : List.of(fastq, bam, vcf)) {
      for (var r : rows) {
        assertThat(portal).contains((String) r.get("portal_run_id"));
        assertThat(libs).contains((String) r.get("library_id"));
        assertThat((String) r.get("cohort_id")).isNotNull();
        assertThat((String) r.get("bucket")).isNotNull();
      }
    }
    assertThat(fastq).allMatch(r -> r.get(Fastq.FASTQ.SEQUENCING_RUN_ID) != null);
  }

  @Test
  void boundaryRowsAndNulls() {
    var ctx = new SeedContext(100, 42);
    var plan = SeedPlan.of(ctx);
    var fastq = seed(new FastqSeeder(), ctx, plan);
    assertThat(fastq.get(0).get(Fastq.FASTQ.SIZE)).isEqualTo(0L);
    assertThat(fastq.get(1).get(Fastq.FASTQ.SIZE)).isEqualTo(Long.MAX_VALUE);
    assertThat(fastq.get(0).get(Fastq.FASTQ.SEQUENCING_RUN_DATE)).isEqualTo(Boundaries.dateMin());
    assertThat(fastq.get(1).get(Fastq.FASTQ.LAST_MODIFIED_DATE))
        .isEqualTo(Boundaries.timestampMax());
    assertThat(fastq).anyMatch(r -> r.get(Fastq.FASTQ.E_TAG) == null);
    assertThat(fastq.get(2).get(Fastq.FASTQ.FILENAME)).endsWith("_R1.fastq.gz");
    var bam = seed(new BamSeeder(), ctx, plan);
    assertThat(bam.get(1).get(Bam.BAM.SIZE)).isEqualTo(Long.MAX_VALUE);
    var vcf = seed(new VcfSeeder(), ctx, plan);
    assertThat(vcf.get(0).get(Vcf.VCF.LAST_MODIFIED_DATE)).isEqualTo(Boundaries.timestampMin());
  }

  @Test
  void tinyScaleFits() {
    var ctx = new SeedContext(1, 42);
    var plan = SeedPlan.of(ctx);
    assertThat(seed(new FastqSeeder(), ctx, plan)).hasSize(4);
    assertThat(seed(new BamSeeder(), ctx, plan)).hasSize(4);
  }
}
