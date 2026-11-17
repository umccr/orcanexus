package dev.ccgcm.platform.orcanexus.mockdb.seed;

/** Thrown when the relationship spec names a table or column that the jOOQ model lacks. */
public final class SpecMismatchException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String table;
  private final String column;

  /**
   * @param table the table named by the spec
   * @param column the missing column, or {@code null} when the table itself is missing
   */
  public SpecMismatchException(String table, String column) {
    super(
        column == null
            ? "Relationship spec names table '" + table + "', which does not exist in the model"
            : "Relationship spec names column '"
                + column
                + "' of table '"
                + table
                + "', which does not exist in the model");
    this.table = table;
    this.column = column;
  }

  public String table() {
    return table;
  }

  /** The missing column; {@code null} when the whole table is missing. */
  public String column() {
    return column;
  }
}
