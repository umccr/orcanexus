package dev.ccgcm.platform.orcanexus.mockdb.testsupport;

import dev.ccgcm.platform.orcanexus.mockdb.config.MockdbConfig;
import dev.ccgcm.platform.orcanexus.mockdb.db.Connector;
import org.jooq.DSLContext;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Recreates the scratch database before a test class and drops it afterwards.
 *
 * <pre>{@code
 * @EnabledIfSystemProperty(named = "mockdb.it", matches = "true")
 * class MyDbTest {
 *   @RegisterExtension static final ScratchDatabaseExtension DB = new ScratchDatabaseExtension();
 * }
 * }</pre>
 */
public final class ScratchDatabaseExtension implements BeforeAllCallback, AfterAllCallback {

  private ScratchDatabase database;

  @Override
  public void beforeAll(ExtensionContext context) {
    database = ScratchDatabase.create(System.getenv());
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (database != null) {
      database.close();
      database = null;
    }
  }

  /** The scratch database; available from {@code @BeforeAll} onwards. */
  public ScratchDatabase database() {
    if (database == null) {
      throw new IllegalStateException("Scratch database is only available while the class runs.");
    }
    return database;
  }

  public MockdbConfig config() {
    return database().config();
  }

  public Connector connector() {
    return database().connector();
  }

  public DSLContext dsl() {
    return database().dsl();
  }
}
