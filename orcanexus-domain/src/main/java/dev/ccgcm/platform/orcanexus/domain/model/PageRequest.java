package dev.ccgcm.platform.orcanexus.domain.model;

public record PageRequest(int page, int size) {

  public PageRequest {
    if (page < 0) {
      throw new IllegalArgumentException("page must not be negative");
    }

    if (size < 1) {
      throw new IllegalArgumentException("size must be at least 1");
    }
  }

  public long offset() {
    return (long) page * size;
  }
}
