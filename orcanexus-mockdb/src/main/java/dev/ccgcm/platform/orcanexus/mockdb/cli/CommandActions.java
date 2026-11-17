package dev.ccgcm.platform.orcanexus.mockdb.cli;

import java.sql.Connection;

/** The work behind each subcommand. Each method returns the process exit code. */
public interface CommandActions {

  /** Creates the schema and tables if they do not exist. */
  int schema(Connection connection, Output out);

  /** Drops the schema and recreates it empty. */
  int reset(Connection connection, Output out);

  /** Inserts synthetic rows. */
  int seed(Connection connection, Output out, int scale, int seed);

  /** Runs reset, then schema, then seed. */
  int up(Connection connection, Output out, int scale, int seed);

  /** Placeholder used until the real actions are wired in. */
  static CommandActions notImplemented() {
    return new CommandActions() {
      @Override
      public int schema(Connection connection, Output out) {
        return fail(out, "schema");
      }

      @Override
      public int reset(Connection connection, Output out) {
        return fail(out, "reset");
      }

      @Override
      public int seed(Connection connection, Output out, int scale, int seed) {
        return fail(out, "seed");
      }

      @Override
      public int up(Connection connection, Output out, int scale, int seed) {
        return fail(out, "up");
      }

      private int fail(Output out, String name) {
        out.err("Subcommand '" + name + "' is not implemented yet.");
        return 1;
      }
    };
  }
}
