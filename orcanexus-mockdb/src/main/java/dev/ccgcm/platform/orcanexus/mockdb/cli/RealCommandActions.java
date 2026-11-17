package dev.ccgcm.platform.orcanexus.mockdb.cli;

import dev.ccgcm.platform.orcanexus.mockdb.db.SchemaService;
import dev.ccgcm.platform.orcanexus.mockdb.seed.SeedRunner;
import java.sql.Connection;

/** Production wiring of the subcommands to {@link SchemaService} and {@link SeedRunner}. */
public final class RealCommandActions implements CommandActions {

  /** A schema-level step (reset or schema) run on one connection. */
  @FunctionalInterface
  public interface SchemaStep {
    int run(Connection connection, Output out);
  }

  /** The seed step. */
  @FunctionalInterface
  public interface SeedStep {
    int run(Connection connection, Output out, int scale, int seed);
  }

  private final SchemaStep resetStep;
  private final SchemaStep schemaStep;
  private final SeedStep seedStep;

  /** Production constructor. */
  public RealCommandActions() {
    this(new SchemaService(), new SeedRunner());
  }

  private RealCommandActions(SchemaService schemaService, SeedRunner seedRunner) {
    this(schemaService::reset, schemaService::schema, seedRunner::run);
  }

  /** Test seam: supply the three steps used by {@code up} and the individual commands. */
  public RealCommandActions(SchemaStep resetStep, SchemaStep schemaStep, SeedStep seedStep) {
    this.resetStep = resetStep;
    this.schemaStep = schemaStep;
    this.seedStep = seedStep;
  }

  @Override
  public int schema(Connection connection, Output out) {
    return schemaStep.run(connection, out);
  }

  @Override
  public int reset(Connection connection, Output out) {
    return resetStep.run(connection, out);
  }

  @Override
  public int seed(Connection connection, Output out, int scale, int seed) {
    return seedStep.run(connection, out, scale, seed);
  }

  @Override
  public int up(Connection connection, Output out, int scale, int seed) {
    int rc = resetStep.run(connection, out);
    if (rc != 0) {
      return failed(out, "reset", rc);
    }
    rc = schemaStep.run(connection, out);
    if (rc != 0) {
      return failed(out, "schema", rc);
    }
    rc = seedStep.run(connection, out, scale, seed);
    if (rc != 0) {
      return failed(out, "seed", rc);
    }
    return 0;
  }

  private static int failed(Output out, String step, int rc) {
    out.err("up: step '" + step + "' failed; remaining steps skipped.");
    return rc;
  }
}
