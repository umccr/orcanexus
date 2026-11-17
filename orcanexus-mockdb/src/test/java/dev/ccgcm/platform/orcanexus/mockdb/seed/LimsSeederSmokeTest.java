package dev.ccgcm.platform.orcanexus.mockdb.seed;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccgcm.platform.orcanexus.jooq.tables.Lims;
import dev.ccgcm.platform.orcanexus.jooq.tables.records.LimsRecord;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class LimsSeederSmokeTest {

  private static List<LimsRecord> seed(int scale, List<SeedNote> notes) {
    var ctx = new SeedContext(scale, 42);
    var rows = new ArrayList<LimsRecord>();
    new LimsSeeder()
        .rows(SeedPlan.of(ctx), ctx, RelationshipSpec.defaults(), notes)
        .forEachRemaining(rows::add);
    return rows;
  }

  @Test
  void yieldsOneUniqueLibraryPerRowWithTiesAndNoBoundaryNote() {
    var notes = new ArrayList<SeedNote>();
    var rows = seed(100, notes);
    assertThat(rows).hasSize(100);
    assertThat(new HashSet<>(rows.stream().map(r -> r.get(Lims.LIMS.LIBRARY_ID)).toList()))
        .hasSize(100);
    assertThat(rows).allMatch(r -> r.get(Lims.LIMS.SAMPLE_ID) != null);
    assertThat(rows.get(0).get(Lims.LIMS.TYPE)).isEqualTo(rows.get(1).get(Lims.LIMS.TYPE));
    assertThat(rows.get(2).get(Lims.LIMS.TYPE)).isEqualTo(rows.get(3).get(Lims.LIMS.TYPE));
    assertThat(rows.get(0).get(Lims.LIMS.TYPE)).isNotEqualTo(rows.get(2).get(Lims.LIMS.TYPE));
    assertThat(rows.get(0).get(Lims.LIMS.SEQUENCING_RUN_DATE)).isEqualTo(SeedPlan.RANGE_START);
    assertThat(notes).isEmpty();
  }

  @Test
  void tinyScaleSkipsBoundariesWithNotes() {
    var notes = new ArrayList<SeedNote>();
    assertThat(seed(2, notes)).hasSize(2);
    assertThat(notes)
        .extracting(SeedNote::column)
        .containsExactly("sequencing_run_date", "load_datetime");
  }
}
