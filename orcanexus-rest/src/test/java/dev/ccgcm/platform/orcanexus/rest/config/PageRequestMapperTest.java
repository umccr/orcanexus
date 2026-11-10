package dev.ccgcm.platform.orcanexus.rest.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccgcm.platform.orcanexus.domain.model.SortDirection;
import dev.ccgcm.platform.orcanexus.domain.model.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class PageRequestMapperTest {

  private final PageRequestMapper mapper = new PageRequestMapper();

  @Test
  void mapsPageAndSizeWithoutSort() {
    var request = mapper.toDomain(PageRequest.of(3, 25));

    assertThat(request.page()).isEqualTo(3);
    assertThat(request.size()).isEqualTo(25);
    assertThat(request.sort()).isEmpty();
  }

  @Test
  void mapsSortOrdersInOrderWithDirections() {
    var request =
        mapper.toDomain(PageRequest.of(0, 10, Sort.by(Sort.Order.asc("a"), Sort.Order.desc("b"))));

    assertThat(request.sort())
        .containsExactly(
            new SortOrder("a", SortDirection.ASC), new SortOrder("b", SortDirection.DESC));
  }
}
