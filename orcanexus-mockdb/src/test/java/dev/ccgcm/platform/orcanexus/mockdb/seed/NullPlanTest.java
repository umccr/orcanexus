package dev.ccgcm.platform.orcanexus.mockdb.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.BitSet;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NullPlanTest {

  private static final int SEEDS = 200;

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 9})
  void smallTablesGetExactlyOneNull(int rows) {
    for (long seed = 0; seed < SEEDS; seed++) {
      var plan = NullPlan.of(rows, null, new Random(seed));
      assertThat(plan.nullCount()).isEqualTo(1);
      assertThat(plan.rows()).isEqualTo(rows);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {10, 11, 25, 100, 1000})
  void largerTablesGetTenToFiftyPercent(int rows) {
    int low = (int) Math.ceil(0.10 * rows);
    int high = (int) Math.floor(0.50 * rows);
    for (long seed = 0; seed < SEEDS; seed++) {
      int k = NullPlan.of(rows, null, new Random(seed)).nullCount();
      assertThat(k).isBetween(low, high);
    }
  }

  @Test
  void targetCountClampsFraction() {
    assertThat(NullPlan.targetCount(100, 0.10)).isEqualTo(10);
    assertThat(NullPlan.targetCount(100, 0.30)).isEqualTo(30);
    assertThat(NullPlan.targetCount(100, 0.0)).isEqualTo(10);
    assertThat(NullPlan.targetCount(100, 0.9)).isEqualTo(50);
    assertThat(NullPlan.targetCount(11, 0.0)).isEqualTo(2);
    assertThat(NullPlan.targetCount(9, 0.9)).isEqualTo(1);
  }

  @ParameterizedTest
  @ValueSource(ints = {3, 10, 25, 100})
  void protectedRowsAreNeverNull(int rows) {
    var protectedRows = new BitSet();
    protectedRows.set(0);
    protectedRows.set(1);
    for (long seed = 0; seed < SEEDS; seed++) {
      var plan = NullPlan.of(rows, protectedRows, new Random(seed));
      assertThat(plan.isNull(0)).isFalse();
      assertThat(plan.isNull(1)).isFalse();
      assertThat(plan.nullCount()).isPositive();
    }
  }

  @Test
  void allRowsProtectedGivesEmptyPlan() {
    var all = new BitSet();
    all.set(0, 5);
    var plan = NullPlan.of(5, all, new Random(1));
    assertThat(plan.isEmpty()).isTrue();
    assertThat(plan.nullCount()).isZero();
  }

  @Test
  void fewEligibleRowsCapNullCount() {
    var protectedRows = new BitSet();
    protectedRows.set(0, 98);
    var plan = NullPlan.of(100, protectedRows, new Random(7));
    assertThat(plan.nullCount()).isEqualTo(2);
    assertThat(plan.isNull(98)).isTrue();
    assertThat(plan.isNull(99)).isTrue();
  }

  @Test
  void zeroRowsGivesEmptyPlan() {
    assertThat(NullPlan.of(0, null, new Random(1)).isEmpty()).isTrue();
    assertThat(NullPlan.none(0).isEmpty()).isTrue();
  }

  @Test
  void sameSeedGivesSamePlan() {
    for (int rows : new int[] {5, 10, 100}) {
      var a = NullPlan.of(rows, null, new Random(42));
      var b = NullPlan.of(rows, null, new Random(42));
      assertThat(a.toBitSet()).isEqualTo(b.toBitSet());
    }
  }

  @Test
  void differentSeedsEventuallyDiffer() {
    var first = NullPlan.of(100, null, new Random(0)).toBitSet();
    boolean differs = false;
    for (long seed = 1; seed < 20 && !differs; seed++) {
      differs = !first.equals(NullPlan.of(100, null, new Random(seed)).toBitSet());
    }
    assertThat(differs).isTrue();
  }

  @Test
  void noneHasNoNulls() {
    var plan = NullPlan.none(50);
    assertThat(plan.isEmpty()).isTrue();
    assertThat(plan.nullCount()).isZero();
    assertThat(plan.rows()).isEqualTo(50);
    for (int i = 0; i < 50; i++) {
      assertThat(plan.isNull(i)).isFalse();
    }
  }

  @Test
  void indexBoundsAreChecked() {
    var plan = NullPlan.of(5, null, new Random(1));
    assertThatThrownBy(() -> plan.isNull(-1)).isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> plan.isNull(5)).isInstanceOf(IndexOutOfBoundsException.class);
    var none = NullPlan.none(3);
    assertThatThrownBy(() -> none.isNull(3)).isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void negativeRowsAndNullRandomAreRejected() {
    assertThatThrownBy(() -> NullPlan.none(-1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> NullPlan.of(-1, null, new Random(1)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> NullPlan.of(5, null, null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void toBitSetIsADefensiveCopy() {
    var plan = NullPlan.of(10, null, new Random(3));
    int before = plan.nullCount();
    plan.toBitSet().clear();
    assertThat(plan.nullCount()).isEqualTo(before);
  }
}
