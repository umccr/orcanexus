package dev.ccgcm.platform.orcanexus.mockdb.testsupport;

import dev.ccgcm.platform.orcanexus.mockdb.config.HostGuard;
import dev.ccgcm.platform.orcanexus.mockdb.config.MockdbConfig;
import dev.ccgcm.platform.orcanexus.mockdb.db.Connections;
import dev.ccgcm.platform.orcanexus.mockdb.db.Connector;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

/**
 * A throw-away database on the local Compose server, created through the maintenance database. Used
 * only by tests enabled with {@code -Pmockdb-it}; never touches the working database.
 */
public final class ScratchDatabase implements AutoCloseable {

  /** Environment variable naming the scratch database. */
  public static final String NAME_VARIABLE = "MOCKDB_IT_NAME";

  public static final String DEFAULT_NAME = "orcanexus_it";

  static final String MAINTENANCE_DATABASE = "postgres";

  private static final Pattern IDENTIFIER = Pattern.compile("[a-z0-9_]+");

  private final MockdbConfig config;
  private final MockdbConfig maintenance;
  private final Connector connector;

  private ScratchDatabase(MockdbConfig maintenance, MockdbConfig config, Connector connector) {
    this.maintenance = maintenance;
    this.config = config;
    this.connector = connector;
  }

  /** Drops and recreates the scratch database named by {@code env}, using the real driver. */
  public static ScratchDatabase create(Map<String, String> env) {
    return create(env, new Connections());
  }

  static ScratchDatabase create(Map<String, String> env, Connector connector) {
    var base = MockdbConfig.fromEnv(env);
    HostGuard.require(base.host());
    var name = scratchName(env);
    var maintenance =
        new MockdbConfig(
            base.host(), base.port(), MAINTENANCE_DATABASE, base.user(), base.password());
    var scratch = new MockdbConfig(base.host(), base.port(), name, base.user(), base.password());
    var db = new ScratchDatabase(maintenance, scratch, connector);
    db.recreate();
    return db;
  }

  /** Reads and validates the scratch database name from {@code env}. */
  static String scratchName(Map<String, String> env) {
    var raw = env.get(NAME_VARIABLE);
    var name = raw == null || raw.isBlank() ? DEFAULT_NAME : raw.trim();
    return requireIdentifier(name);
  }

  /** Accepts only {@code [a-z0-9_]+}. */
  static String requireIdentifier(String name) {
    if (name == null || !IDENTIFIER.matcher(name).matches()) {
      throw new IllegalArgumentException(
          "Scratch database name must match [a-z0-9_]+, got '" + name + "'.");
    }
    return name;
  }

  /** Settings pointing at the scratch database. */
  public MockdbConfig config() {
    return config;
  }

  /** Opens connections with the real driver (10 second timeouts). */
  public Connector connector() {
    return connector;
  }

  /** Opens a new connection to the scratch database; the caller closes it. */
  public Connection open() throws SQLException {
    return connector.open(config);
  }

  /** A jOOQ context that opens a connection per use; no pooling, nothing to close. */
  public DSLContext dsl() {
    return DSL.using(new ConnectorProvider(connector, config), SQLDialect.POSTGRES);
  }

  private void recreate() {
    try (var conn = openMaintenance()) {
      var dsl = DSL.using(conn, SQLDialect.POSTGRES);
      var ident = DSL.name(config.database());
      dsl.execute("DROP DATABASE IF EXISTS {0} WITH (FORCE)", ident);
      dsl.execute("CREATE DATABASE {0}", ident);
    } catch (SQLException | org.jooq.exception.DataAccessException e) {
      throw new IllegalStateException(
          "Cannot create scratch database '" + config.database() + "': " + e.getMessage(), e);
    }
  }

  private Connection openMaintenance() throws SQLException {
    try {
      return connector.open(maintenance);
    } catch (SQLException e) {
      throw new IllegalStateException(
          "Cannot reach PostgreSQL at %s:%d (maintenance database '%s'). Start the Compose_Stack"
                  .formatted(maintenance.host(), maintenance.port(), MAINTENANCE_DATABASE)
              + " first (nexus_docker_up). Cause: "
              + e.getMessage(),
          e);
    }
  }

  /** Drops the scratch database, best effort. */
  @Override
  public void close() {
    try (var conn = openMaintenance()) {
      DSL.using(conn, SQLDialect.POSTGRES)
          .execute("DROP DATABASE IF EXISTS {0} WITH (FORCE)", DSL.name(config.database()));
    } catch (SQLException | RuntimeException e) {
      System.err.println(
          "Could not drop scratch database '" + config.database() + "': " + e.getMessage());
    }
  }
}
