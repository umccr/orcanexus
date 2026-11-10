package dev.ccgcm.platform.orcanexus.domain.model;

import java.util.List;

public record PageRequest(int page, int size, List<SortOrder> sort) {

  public PageRequest {
    if (page < 0) {
      throw new IllegalArgumentException("page must not be negative");
    }

    if (size < 1) {
      throw new IllegalArgumentException("size must be at least 1");
    }

    sort = List.copyOf(sort);
  }

  public PageRequest(int page, int size) {
    this(page, size, List.of());
  }

  public long offset() {
    return (long) page * size;
  }
}
