package dev.ccgcm.platform.orcanexus.domain.model;

import java.util.Objects;

/** A requested ordering on a single field, expressed in the field name the API exposes. */
public record SortOrder(String field, SortDirection direction) {

  public SortOrder {
    Objects.requireNonNull(field, "field must not be null");
    Objects.requireNonNull(direction, "direction must not be null");

    if (field.isBlank()) {
      throw new IllegalArgumentException("field must not be blank");
    }
  }
}
