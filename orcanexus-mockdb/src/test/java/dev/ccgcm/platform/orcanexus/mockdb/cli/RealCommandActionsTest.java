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
import org.junit.jupiter.params.provider.ValueSource;

/** Orchestration of {@code up} and the individual commands. Validates: 9.2, 9.3, 9.6, 13.4. */
@DisplayName("RealCommandActions orchestration")
class RealCommandActionsTest {

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
  }

  private static final class RecordingConnector implements Connector {
    int calls;

    @Override
    public Connection open(MockdbConfig config) throws SQLException {
      calls++;
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

  /** Fake steps recording call order; a step listed in {@code codes} returns its code. */
  private static final class FakeSteps {
    final List<String> calls = new ArrayList<>();
    final Map<String, Integer> codes;

    FakeSteps(Map<String, Integer> codes) {
      this.codes = codes;
    }

    RealCommandActions actions() {
      return new RealCommandActions(
          (c, o) -> record("reset"),
          (c, o) -> record("schema"),
          (c, o, scale, seed) -> record("seed " + scale + " " + seed));
    }

    private int record(String call) {
      calls.add(call);
      return codes.getOrDefault(call.split(" ")[0], 0);
    }
  }

  private final RecordingOutput output = new RecordingOutput();

  @Test
  void upRunsResetSchemaSeedInOrderWithScaleAndSeed() {
    var steps = new FakeSteps(Map.of());
    assertThat(steps.actions().up(null, output, 5, 7)).isZero();
    assertThat(steps.calls).containsExactly("reset", "schema", "seed 5 7");
    assertThat(output.err).isEmpty();
  }

  @Test
  void failingResetSkipsSchemaAndSeed() {
    var steps = new FakeSteps(Map.of("reset", 3));
    assertThat(steps.actions().up(null, output, 1, 2)).isEqualTo(3);
    assertThat(steps.calls).containsExactly("reset");
    assertThat(output.err).containsExactly("up: step 'reset' failed; remaining steps skipped.");
  }

  @Test
  void failingSchemaSkipsSeed() {
    var steps = new FakeSteps(Map.of("schema", 4));
    assertThat(steps.actions().up(null, output, 1, 2)).isEqualTo(4);
    assertThat(steps.calls).containsExactly("reset", "schema");
    assertThat(output.err).containsExactly("up: step 'schema' failed; remaining steps skipped.");
  }

  @Test
  void failingSeedReturnsItsCode() {
    var steps = new FakeSteps(Map.of("seed", 5));
    assertThat(steps.actions().up(null, output, 1, 2)).isEqualTo(5);
    assertThat(steps.calls).containsExactly("reset", "schema", "seed 1 2");
    assertThat(output.err).containsExactly("up: step 'seed' failed; remaining steps skipped.");
  }

  @Test
  void individualCommandsCallOnlyTheirStep() {
    var steps = new FakeSteps(Map.of());
    var actions = steps.actions();
    assertThat(actions.reset(null, output)).isZero();
    assertThat(actions.schema(null, output)).isZero();
    assertThat(actions.seed(null, output, 9, 8)).isZero();
    assertThat(steps.calls).containsExactly("reset", "schema", "seed 9 8");
    assertThat(output.err).isEmpty();
  }

  @Test
  void individualCommandsReturnStepCodeWithoutUpMessage() {
    var steps = new FakeSteps(Map.of("reset", 2, "schema", 3, "seed", 4));
    var actions = steps.actions();
    assertThat(actions.reset(null, output)).isEqualTo(2);
    assertThat(actions.schema(null, output)).isEqualTo(3);
    assertThat(actions.seed(null, output, 1, 1)).isEqualTo(4);
    assertThat(output.err).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"127.0.0.2", "example.com", "LOCALHOST"})
  void upWithRejectedHostMakesNoConnectionAndNoStepCalls(String host) {
    var steps = new FakeSteps(Map.of());
    var connector = new RecordingConnector();
    var cli = new MockdbCli(steps.actions());
    assertThat(cli.run(new String[] {"up"}, Map.of("MOCKDB_HOST", host), output, connector))
        .isEqualTo(1);
    assertThat(connector.calls).isZero();
    assertThat(steps.calls).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"--scale 0", "--scale abc", "--seed -1", "--bogus"})
  void upWithParserErrorMakesNoConnectionAndNoStepCalls(String args) {
    var steps = new FakeSteps(Map.of());
    var connector = new RecordingConnector();
    var cli = new MockdbCli(steps.actions());
    var argv = ("up " + args).split(" ");
    assertThat(cli.run(argv, Map.of(), output, connector)).isEqualTo(2);
    assertThat(connector.calls).isZero();
    assertThat(steps.calls).isEmpty();
  }

  @Test
  void upEndToEndReachesSeedStepWithScaleAndSeed() {
    var steps = new FakeSteps(Map.of());
    var connector = new RecordingConnector();
    var cli = new MockdbCli(steps.actions());
    var argv = new String[] {"up", "--scale", "5", "--seed", "7"};
    assertThat(cli.run(argv, Map.of(), output, connector)).isZero();
    assertThat(connector.calls).isEqualTo(1);
    assertThat(steps.calls).containsExactly("reset", "schema", "seed 5 7");
  }

  @Test
  void upEndToEndStopsAtFailedStepAndExitsWithItsCode() {
    var steps = new FakeSteps(Map.of("schema", 1));
    var connector = new RecordingConnector();
    var cli = new MockdbCli(steps.actions());
    assertThat(cli.run(new String[] {"up"}, Map.of(), output, connector)).isEqualTo(1);
    assertThat(steps.calls).containsExactly("reset", "schema");
    assertThat(output.err).containsExactly("up: step 'schema' failed; remaining steps skipped.");
  }
}
