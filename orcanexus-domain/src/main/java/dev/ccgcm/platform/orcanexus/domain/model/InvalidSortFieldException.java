package dev.ccgcm.platform.orcanexus.domain.model;

/** Raised when a requested sort field is not one the queried resource can be ordered by. */
public class InvalidSortFieldException extends IllegalArgumentException {

  private static final long serialVersionUID = 1L;

  private final String field;

  public InvalidSortFieldException(String field) {
    super("Unknown sort field: " + field);
    this.field = field;
  }

  public String getField() {
    return field;
  }
}
