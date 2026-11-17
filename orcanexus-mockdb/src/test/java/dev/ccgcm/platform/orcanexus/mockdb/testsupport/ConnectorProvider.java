package dev.ccgcm.platform.orcanexus.mockdb.testsupport;

import dev.ccgcm.platform.orcanexus.mockdb.config.MockdbConfig;
import dev.ccgcm.platform.orcanexus.mockdb.db.Connector;
import java.sql.Connection;
import java.sql.SQLException;
import org.jooq.ConnectionProvider;
import org.jooq.exception.DataAccessException;

/** Hands jOOQ a fresh connection per use and closes it on release. */
final class ConnectorProvider implements ConnectionProvider {

  private final Connector connector;
  private final MockdbConfig config;

  ConnectorProvider(Connector connector, MockdbConfig config) {
    this.connector = connector;
    this.config = config;
  }

  @Override
  public Connection acquire() {
    try {
      return connector.open(config);
    } catch (SQLException e) {
      throw new DataAccessException("Cannot open connection: " + e.getMessage(), e);
    }
  }

  @Override
  public void release(Connection connection) {
    try {
      connection.close();
    } catch (SQLException e) {
      throw new DataAccessException("Cannot close connection: " + e.getMessage(), e);
    }
  }
}
