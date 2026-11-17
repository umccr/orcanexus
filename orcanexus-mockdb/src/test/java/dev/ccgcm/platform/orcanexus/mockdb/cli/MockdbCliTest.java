package dev.ccgcm.platform.orcanexus.mockdb.cli;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccgcm.platform.orcanexus.mockdb.config.MockdbConfig;
import dev.ccgcm.platform.orcanexus.mockdb.db.Connector;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Properties 12 and 14 at CLI level: every rejection leaves the recording Connector untouched.
 * Validates: Requirements 4.3-4.8, 4.10, 6.8, 9.6, 9.7.
 */
@DisplayName("Properties 12 and 14: CLI never connects on rejection")
class MockdbCliTest {

  private static final class RecordingConnector implements Connector {
    final List<MockdbConfig> calls = new ArrayList<>();
    SQLException failure;

    @Override
    public Connection open(MockdbConfig config) throws SQLException {
      calls.add(config);
      if (failure != null) {
        throw failure;
      }
      return (Connection)
          Proxy.newProxyInstance(
              getClass().getClassLoader(),
              new Class<?>[] {Connection.class},
              (proxy, method, args) -> {
                if ("close".equals(method.getName())) {
                  return null;
                }
                throw new UnsupportedOperationException(method.getName());
              });
    }
  }

  private static final class RecordingOutput implements Output {
    final List<String> out = new ArrayList<>();
    final List<String> err = new ArrayList<>();

    @Override
    public void out(String line) {
      out.add(line);
    }

    @Override
    public void err(String line) {
      err.add(line);
    }

    String allErr() {
      return String.join("\n", err);
    }
  }

  private static final class RecordingActions implements CommandActions {
    final List<String> calls = new ArrayList<>();
    int exit;

    @Override
    public int schema(Connection c, Output o) {
      calls.add("schema");
      return exit;
    }

    @Override
    public int reset(Connection c, Output o) {
      calls.add("reset");
      return exit;
    }

    @Override
    public int seed(Connection c, Output o, int scale, int seed) {
      calls.add("seed " + scale + " " + seed);
      return exit;
    }

    @Override
    public int up(Connection c, Output o, int scale, int seed) {
      calls.add("up " + scale + " " + seed);
      return exit;
    }
  }

  private final RecordingConnector connector = new RecordingConnector();
  private final RecordingOutput output = new RecordingOutput();
  private final RecordingActions actions = new RecordingActions();
  private final MockdbCli cli = new MockdbCli(actions);

  private int run(Map<String, String> env, String... args) {
    return cli.run(args, env, output, connector);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "frobnicate",
        "seed --scale 0",
        "seed --scale 100001",
        "seed --scale abc",
        "up --seed -1",
        "seed --scale",
        "seed --scale 1 --scale 2",
        "schema --scale 5",
        "reset --seed 5",
        "seed --bogus",
      })
  void parserRejectionsExit2WithoutConnecting(String line) {
    assertThat(run(Map.of(), line.split(" "))).isEqualTo(2);
    assertThat(connector.calls).isEmpty();
    assertThat(actions.calls).isEmpty();
    assertThat(output.allErr()).contains("Error:").contains("Usage:");
  }

  @Test
  void noSubcommandExitsNonZeroWithoutConnecting() {
    assertThat(run(Map.of())).isEqualTo(2);
    assertThat(connector.calls).isEmpty();
    assertThat(output.allErr()).contains("missing subcommand");
  }

  @ParameterizedTest
  @ValueSource(strings = {"--help", "-h"})
  void helpExitsZeroPrintsUsageWithoutConnecting(String flag) {
    assertThat(run(Map.of(), flag)).isZero();
    assertThat(connector.calls).isEmpty();
    assertThat(output.out).hasSize(1);
    assertThat(output.out.get(0)).contains("schema", "seed", "reset", "up", "--scale", "--seed");
    assertThat(output.err).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "127.0.0.2",
        "0.0.0.0",
        "example.com",
        "orca.cluster-abc.ap-southeast-2.rds.amazonaws.com",
        "LOCALHOST",
        "localhost.",
        " 127.0.0.1"
      })
  void rejectedHostNeverConnectsForAnyCommand(String host) {
    for (var command : List.of("schema", "seed", "reset", "up")) {
      assertThat(run(Map.of("MOCKDB_HOST", host), command)).isEqualTo(1);
    }
    assertThat(connector.calls).isEmpty();
    assertThat(actions.calls).isEmpty();
    assertThat(output.allErr()).contains("'" + host + "'");
  }

  @Test
  void tunnelHostVariableIsIgnoredAndRemoteDbHostNeverUsed() {
    var env = Map.of("DB_HOST", "aurora.example.rds.amazonaws.com");
    assertThat(run(env, "schema")).isZero();
    assertThat(connector.calls).hasSize(1);
    assertThat(connector.calls.get(0).host()).isEqualTo("127.0.0.1");
  }

  @Test
  void badPortExits1WithoutConnecting() {
    assertThat(run(Map.of("MOCKDB_PORT", "abc"), "schema")).isEqualTo(1);
    assertThat(connector.calls).isEmpty();
    assertThat(output.allErr()).contains("MOCKDB_PORT");
  }

  @ParameterizedTest
  @ValueSource(strings = {"localhost", "127.0.0.1", "::1"})
  void acceptedHostConnectsOnce(String host) {
    assertThat(run(Map.of("MOCKDB_HOST", host), "schema")).isZero();
    assertThat(connector.calls).hasSize(1);
    assertThat(connector.calls.get(0).host()).isEqualTo(host);
  }

  @Test
  void connectionFailureReportsMessageAndExit1() {
    connector.failure = new SQLException("Cannot reach the mock database at 127.0.0.1:5433.");
    assertThat(run(Map.of(), "seed")).isEqualTo(1);
    assertThat(connector.calls).hasSize(1);
    assertThat(output.allErr()).contains("Cannot reach the mock database at 127.0.0.1:5433");
    assertThat(actions.calls).isEmpty();
  }

  @Test
  void commandsDispatchWithScaleAndSeedPassedThrough() {
    assertThat(run(Map.of(), "schema")).isZero();
    assertThat(run(Map.of(), "reset")).isZero();
    assertThat(run(Map.of(), "seed", "--scale", "7", "--seed", "9")).isZero();
    assertThat(run(Map.of(), "up", "--seed", "3", "--scale", "11")).isZero();
    assertThat(run(Map.of(), "seed")).isZero();
    assertThat(actions.calls)
        .containsExactly("schema", "reset", "seed 7 9", "up 11 3", "seed 100 42");
    assertThat(connector.calls).hasSize(5);
  }

  @Test
  void actionExitCodeIsReturned() {
    actions.exit = 1;
    assertThat(run(Map.of(), "schema")).isEqualTo(1);
    actions.exit = 0;
    assertThat(run(Map.of(), "schema")).isZero();
  }

  @Test
  void defaultActionsReportNotImplemented() {
    var rc = new MockdbCli().run(new String[] {"schema"}, Map.of(), output, connector);
    assertThat(rc).isEqualTo(1);
    assertThat(output.allErr()).contains("not implemented");
  }
}
