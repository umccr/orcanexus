package dev.ccgcm.platform.orcanexus.domain.model;

import java.util.List;

public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

  public PageResult {
    content = List.copyOf(content);
  }

  public long totalPages() {
    return (totalElements + size - 1) / size;
  }

  public boolean hasNext() {
    return page + 1 < totalPages();
  }

  public boolean hasPrevious() {
    return page > 0;
  }
}
