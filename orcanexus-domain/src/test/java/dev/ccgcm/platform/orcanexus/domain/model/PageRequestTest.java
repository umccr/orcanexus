package dev.ccgcm.platform.orcanexus.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageRequestTest {

  @Test
  void twoArgumentConstructorHasNoSort() {
    var request = new PageRequest(2, 10);

    assertTrue(request.sort().isEmpty());
    assertEquals(20L, request.offset());
  }

  @Test
  void rejectsNegativePage() {
    assertThrows(IllegalArgumentException.class, () -> new PageRequest(-1, 10));
  }

  @Test
  void rejectsSizeBelowOne() {
    assertThrows(IllegalArgumentException.class, () -> new PageRequest(0, 0));
  }

  @Test
  void rejectsNullSort() {
    assertThrows(NullPointerException.class, () -> new PageRequest(0, 10, null));
  }

  @Test
  void defensivelyCopiesSort() {
    var sort = new ArrayList<>(List.of(new SortOrder("a", SortDirection.ASC)));
    var request = new PageRequest(0, 10, sort);

    sort.add(new SortOrder("b", SortDirection.DESC));

    assertEquals(1, request.sort().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> request.sort().add(new SortOrder("c", SortDirection.ASC)));
  }

  @Test
  void equalRequestsAreEqual() {
    var sort = List.of(new SortOrder("a", SortDirection.DESC));

    assertEquals(new PageRequest(1, 5, sort), new PageRequest(1, 5, sort));
    assertEquals(new PageRequest(1, 5), new PageRequest(1, 5, List.of()));
  }
}
