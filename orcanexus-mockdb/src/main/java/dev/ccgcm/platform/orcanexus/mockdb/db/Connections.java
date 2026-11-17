package dev.ccgcm.platform.orcanexus.mockdb.db;

import dev.ccgcm.platform.orcanexus.mockdb.config.MockdbConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** The real {@link Connector}, backed by the PostgreSQL JDBC driver. */
public final class Connections implements Connector {

  @Override
  public Connection open(MockdbConfig config) throws SQLException {
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      throw new SQLException("PostgreSQL JDBC driver is not on the classpath.", e);
    }
    try {
      return DriverManager.getConnection(jdbcUrl(config), properties(config));
    } catch (SQLException e) {
      throw translate(config, e);
    }
  }

  /** Builds the JDBC URL without credentials; IPv6 literals are bracketed. */
  static String jdbcUrl(MockdbConfig config) {
    var host = config.host().contains(":") ? "[" + config.host() + "]" : config.host();
    return "jdbc:postgresql://" + host + ":" + config.port() + "/" + config.database();
  }

  static Properties properties(MockdbConfig config) {
    var props = new Properties();
    props.setProperty("user", config.user());
    props.setProperty("password", config.password());
    props.setProperty("connectTimeout", "10");
    props.setProperty("loginTimeout", "10");
    props.setProperty("reWriteBatchedInserts", "true");
    props.setProperty("ApplicationName", "orcanexus-mockdb");
    return props;
  }

  /** Maps driver failures to friendly messages; unknown failures pass through unchanged. */
  static SQLException translate(MockdbConfig config, SQLException e) {
    var state = e.getSQLState();
    if ("28P01".equals(state)) {
      return new SQLException(
          "Authentication failed for user '"
              + config.user()
              + "'. Check MOCKDB_USER and MOCKDB_PASSWORD.",
          state,
          e);
    }
    var message = e.getMessage() == null ? "" : e.getMessage().toLowerCase(java.util.Locale.ROOT);
    boolean unreachable =
        "08001".equals(state)
            || "08006".equals(state)
            || "08004".equals(state)
            || message.contains("connection refused");
    if (unreachable) {
      return new SQLException(
          "Cannot reach the mock database at "
              + config.host()
              + ":"
              + config.port()
              + ". Start the Compose_Stack first (nexus_docker_up).",
          state,
          e);
    }
    return e;
  }
}
