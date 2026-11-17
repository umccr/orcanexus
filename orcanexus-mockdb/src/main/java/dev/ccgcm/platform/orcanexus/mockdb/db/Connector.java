package dev.ccgcm.platform.orcanexus.mockdb.db;

import dev.ccgcm.platform.orcanexus.mockdb.config.MockdbConfig;
import java.sql.Connection;
import java.sql.SQLException;

/** Opens a connection to the mock database. Tests substitute a fake. */
public interface Connector {

  Connection open(MockdbConfig config) throws SQLException;
}
