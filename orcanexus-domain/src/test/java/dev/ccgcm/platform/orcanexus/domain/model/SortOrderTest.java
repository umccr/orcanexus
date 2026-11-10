package dev.ccgcm.platform.orcanexus.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SortOrderTest {

  @Test
  void holdsFieldAndDirection() {
    var order = new SortOrder("library_id", SortDirection.DESC);

    assertEquals("library_id", order.field());
    assertEquals(SortDirection.DESC, order.direction());
  }

  @Test
  void rejectsBlankField() {
    assertThrows(IllegalArgumentException.class, () -> new SortOrder(" ", SortDirection.ASC));
    assertThrows(IllegalArgumentException.class, () -> new SortOrder("", SortDirection.ASC));
  }

  @Test
  void rejectsNulls() {
    assertThrows(NullPointerException.class, () -> new SortOrder(null, SortDirection.ASC));
    assertThrows(NullPointerException.class, () -> new SortOrder("a", null));
  }

  @Test
  void invalidSortFieldExceptionNamesTheField() {
    var exception = new InvalidSortFieldException("nope");

    assertEquals("nope", exception.getField());
    assertEquals("Unknown sort field: nope", exception.getMessage());
  }
}
