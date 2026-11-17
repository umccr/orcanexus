package dev.ccgcm.platform.orcanexus.mockdb.config;

import java.util.Map;

/** Connection settings for the local mock database, read only from {@code MOCKDB_*}. */
public record MockdbConfig(String host, int port, String database, String user, String password) {

  static final String DEFAULT_HOST = "127.0.0.1";
  static final int DEFAULT_PORT = 5433;
  static final String DEFAULT_DATABASE = "orcanexus";
  static final String DEFAULT_USER = "orcanexus";

  /** Documented local-only default; never a real credential. */
  static final String DEFAULT_PASSWORD = "mockdb-local-only"; // pragma: allowlist secret

  /** Builds the config from the given environment, reading only {@code MOCKDB_*} entries. */
  public static MockdbConfig fromEnv(Map<String, String> env) {
    return new MockdbConfig(
        value(env, "MOCKDB_HOST", DEFAULT_HOST),
        parsePort(env.get("MOCKDB_PORT")),
        value(env, "MOCKDB_NAME", DEFAULT_DATABASE),
        value(env, "MOCKDB_USER", DEFAULT_USER),
        value(env, "MOCKDB_PASSWORD", DEFAULT_PASSWORD));
  }

  private static String value(Map<String, String> env, String key, String fallback) {
    var v = env.get(key);
    return v == null || v.isBlank() ? fallback : v;
  }

  private static int parsePort(String raw) {
    if (raw == null || raw.isBlank()) {
      return DEFAULT_PORT;
    }
    try {
      int port = Integer.parseInt(raw.trim());
      if (port >= 1 && port <= 65535) {
        return port;
      }
    } catch (NumberFormatException e) {
      // fall through to the shared message
    }
    throw new IllegalArgumentException(
        "MOCKDB_PORT must be an integer between 1 and 65535, got '" + raw + "'.");
  }

  @Override
  public String toString() {
    return "MockdbConfig[host=%s, port=%d, database=%s, user=%s]"
        .formatted(host, port, database, user);
  }
}
