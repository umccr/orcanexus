package dev.ccgcm.platform.orcanexus.mockdb.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.TableRecord;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;
import org.jooq.impl.TableRecordImpl;
import org.junit.jupiter.api.Test;

class GenericSeederTest {

  static final class FakeRecord extends TableRecordImpl<FakeRecord> {
    private static final long serialVersionUID = 1L;

    FakeRecord() {
      super(FakeTable.FAKE);
    }
  }

  static final class FakeTable extends TableImpl<FakeRecord> {
    private static final long serialVersionUID = 1L;
    static final FakeTable FAKE = new FakeTable();
    final TableField<FakeRecord, String> txt = createField(DSL.name("txt"), SQLDataType.CLOB);
    final TableField<FakeRecord, LocalDate> d = createField(DSL.name("d"), SQLDataType.LOCALDATE);
    final TableField<FakeRecord, LocalDateTime> ts =
        createField(DSL.name("ts"), SQLDataType.LOCALDATETIME(6));
    final TableField<FakeRecord, Long> big = createField(DSL.name("big"), SQLDataType.BIGINT);
    final TableField<FakeRecord, BigDecimal> num =
        createField(DSL.name("num"), SQLDataType.NUMERIC(10, 3));
    final TableField<FakeRecord, Boolean> flag = createField(DSL.name("flag"), SQLDataType.BOOLEAN);
    final TableField<FakeRecord, Integer> i = createField(DSL.name("i"), SQLDataType.INTEGER);

    private FakeTable() {
      super(DSL.name("fake_table"));
    }

    @Override
    public Class<? extends FakeRecord> getRecordType() {
      return FakeRecord.class;
    }
  }

  private static List<TableRecord<?>> seed(int scale, List<SeedNote> notes) {
    var ctx = new SeedContext(scale, 7);
    var rows = new ArrayList<TableRecord<?>>();
    new GenericSeeder(FakeTable.FAKE)
        .rows(SeedPlan.of(ctx), ctx, RelationshipSpec.defaults(), notes)
        .forEachRemaining(rows::add);
    return rows;
  }

  @Test
  void generatesTypeValidDeterministicRowsWithNullsAndBoundaries() {
    var notes = new ArrayList<SeedNote>();
    var rows = seed(100, notes);
    var t = FakeTable.FAKE;
    assertThat(rows).hasSize(100);
    assertThat(notes).hasSize(1);
    assertThat(notes.get(0).kind()).isEqualTo(SeedNote.Kind.NO_SPEC);
    assertThat(notes.get(0).table()).isEqualTo("fake_table");
    for (Field<?> f : t.fields()) {
      long nulls = rows.stream().filter(r -> r.get(f) == null).count();
      assertThat(nulls).as(f.getName()).isBetween(10L, 50L);
    }
    assertThat(rows.get(0).get(t.big)).isZero();
    assertThat(rows.get(1).get(t.big)).isEqualTo(Long.MAX_VALUE);
    assertThat(rows.get(1).get(t.num)).isEqualByComparingTo("9999999.999");
    assertThat(rows.get(0).get(t.d)).isEqualTo(Boundaries.dateMin());
    assertThat(rows.stream().map(r -> r.get(t.txt)).filter(s -> s != null))
        .allMatch(s -> s.startsWith("MOCK"));
    assertThat(seed(100, new ArrayList<>()).toString()).isEqualTo(rows.toString());
  }

  @Test
  void tinyScaleUsesFourRowsAndOneNull() {
    var rows = seed(1, new ArrayList<>());
    assertThat(rows).hasSize(4);
    assertThat(rows.stream().filter(r -> r.get(FakeTable.FAKE.txt) == null).count()).isEqualTo(1);
  }

  @Test
  void fillUnsetOnlyTouchesUnchangedFields() {
    var t = FakeTable.FAKE;
    var record = new FakeRecord();
    record.set(t.txt, null);
    record.set(t.big, 5L);
    GenericValues.fillUnset(record, new Random(1), 3);
    assertThat(record.get(t.txt)).isNull();
    assertThat(record.get(t.big)).isEqualTo(5L);
    assertThat(record.get(t.num)).isNotNull();
    assertThat(record.get(t.num).scale()).isEqualTo(3);
    assertThat(record.get(t.num).precision()).isLessThanOrEqualTo(10);
    assertThat(record.get(t.d)).isNotNull();
    assertThat(record.get(t.ts)).isNotNull();
    assertThat(record.get(t.flag)).isNotNull();
    assertThat(record.get(t.i)).isNotNull();
    for (Field<?> f : t.fields()) {
      assertThat(record.changed(f)).as(f.getName()).isTrue();
    }
  }
}
