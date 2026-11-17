package dev.ccgcm.platform.orcanexus.mockdb;

import dev.ccgcm.platform.orcanexus.mockdb.cli.MockdbCli;
import dev.ccgcm.platform.orcanexus.mockdb.cli.Output;
import dev.ccgcm.platform.orcanexus.mockdb.cli.RealCommandActions;
import dev.ccgcm.platform.orcanexus.mockdb.db.Connections;

/** Entry point of the dev-only mock database CLI. */
public final class MockdbMain {

  private MockdbMain() {}

  /** Writes to the process standard streams. */
  private static final class ConsoleOutput implements Output {
    @Override
    public void out(String line) {
      System.out.println(line);
    }

    @Override
    public void err(String line) {
      System.err.println(line);
    }
  }

  /**
   * Process entry point; the only place that exits.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    // Quiet jOOQ's banner and tips; must be set before the first jOOQ class loads.
    System.setProperty("org.jooq.no-logo", "true");
    System.setProperty("org.jooq.no-tips", "true");
    int code =
        new MockdbCli(new RealCommandActions())
            .run(args, System.getenv(), new ConsoleOutput(), new Connections());
    System.exit(code);
  }
}
