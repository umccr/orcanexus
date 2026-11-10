package dev.ccgcm.platform.orcanexus.aurora.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.ccgcm.platform.orcanexus.domain.model.InvalidSortFieldException;
import dev.ccgcm.platform.orcanexus.domain.model.SortDirection;
import dev.ccgcm.platform.orcanexus.domain.model.SortOrder;
import dev.ccgcm.platform.orcanexus.jooq.tables.Lims;
import java.util.List;
import org.jooq.SortField;
import org.junit.jupiter.api.Test;

/** Exercises the ordering logic only; no database connection is involved. */
class LimsRepositoryOrderByTest {

  private static final String FIRST_COLUMN = Lims.LIMS.fields()[0].getName();
  private static final String OTHER_COLUMN = Lims.LIMS.LIBRARY_ID.getName();
  private static final String THIRD_COLUMN = Lims.LIMS.SEQUENCING_RUN_DATE.getName();

  private static List<String> describe(List<SortField<?>> orderBy) {
    return orderBy.stream().map(f -> f.getName() + " " + f.getOrder()).toList();
  }

  @Test
  void noSortKeepsFirstColumnAscending() {
    var orderBy = LimsRepository.orderBy(Lims.LIMS, List.of());

    assertThat(describe(orderBy)).containsExactly(FIRST_COLUMN + " ASC");
  }

  @Test
  void singleSortAddsTiebreaker() {
    var orderBy =
        LimsRepository.orderBy(Lims.LIMS, List.of(new SortOrder(OTHER_COLUMN, SortDirection.DESC)));

    assertThat(describe(orderBy)).containsExactly(OTHER_COLUMN + " DESC", FIRST_COLUMN + " ASC");
  }

  @Test
  void multipleSortsKeepRequestedOrderThenTiebreaker() {
    var orderBy =
        LimsRepository.orderBy(
            Lims.LIMS,
            List.of(
                new SortOrder(OTHER_COLUMN, SortDirection.ASC),
                new SortOrder(THIRD_COLUMN, SortDirection.DESC)));

    assertThat(describe(orderBy))
        .containsExactly(OTHER_COLUMN + " ASC", THIRD_COLUMN + " DESC", FIRST_COLUMN + " ASC");
  }

  @Test
  void tiebreakerIsNotDuplicatedWhenFirstColumnIsRequested() {
    var orderBy =
        LimsRepository.orderBy(Lims.LIMS, List.of(new SortOrder(FIRST_COLUMN, SortDirection.DESC)));

    assertThat(describe(orderBy)).containsExactly(FIRST_COLUMN + " DESC");
  }

  @Test
  void firstRequestWinsForRepeatedField() {
    var orderBy =
        LimsRepository.orderBy(
            Lims.LIMS,
            List.of(
                new SortOrder(OTHER_COLUMN, SortDirection.ASC),
                new SortOrder(OTHER_COLUMN, SortDirection.DESC)));

    assertThat(describe(orderBy)).containsExactly(OTHER_COLUMN + " ASC", FIRST_COLUMN + " ASC");
  }

  @Test
  void rejectsUnknownField() {
    var sort = List.of(new SortOrder("not_a_column", SortDirection.ASC));

    assertThatThrownBy(() -> LimsRepository.orderBy(Lims.LIMS, sort))
        .isInstanceOfSatisfying(
            InvalidSortFieldException.class,
            e -> assertThat(e.getField()).isEqualTo("not_a_column"));
  }

  @Test
  void fieldMatchIsCaseSensitive() {
    var sort = List.of(new SortOrder(OTHER_COLUMN.toUpperCase(), SortDirection.ASC));

    assertThatThrownBy(() -> LimsRepository.orderBy(Lims.LIMS, sort))
        .isInstanceOf(InvalidSortFieldException.class);
  }

  @Test
  void rejectsInjectionAttemptsAsUnknownFields() {
    var sort = List.of(new SortOrder("library_id; drop table lims", SortDirection.ASC));

    assertThatThrownBy(() -> LimsRepository.orderBy(Lims.LIMS, sort))
        .isInstanceOf(InvalidSortFieldException.class);
  }
}
