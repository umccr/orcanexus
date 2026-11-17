package dev.ccgcm.platform.orcanexus.mockdb.cli;

import dev.ccgcm.platform.orcanexus.mockdb.config.HostGuard;
import dev.ccgcm.platform.orcanexus.mockdb.config.HostRejectedException;
import dev.ccgcm.platform.orcanexus.mockdb.config.MockdbConfig;
import dev.ccgcm.platform.orcanexus.mockdb.db.Connector;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

/**
 * Orchestrates one run: parse, configure, guard the host, connect, dispatch.
 *
 * <p>Exit codes: 0 success or {@code --help}, 1 runtime failure (including an invalid {@code
 * MOCKDB_*} environment value, a rejected host and a failed connection), 2 usage error. Never calls
 * {@code System.exit}.
 */
public final class MockdbCli {

  static final int EXIT_FAILURE = 1;

  private final CommandActions actions;

  /** A CLI whose subcommands report "not implemented yet". */
  public MockdbCli() {
    this(CommandActions.notImplemented());
  }

  /** A CLI that dispatches to the given actions. */
  public MockdbCli(CommandActions actions) {
    this.actions = Objects.requireNonNull(actions, "actions");
  }

  /** Runs the tool and returns the process exit code. */
  public int run(String[] args, Map<String, String> env, Output out, Connector connector) {
    Invocation invocation;
    try {
      invocation = ArgParser.parse(args);
    } catch (UsageException e) {
      if (e.isHelp()) {
        out.out(ArgParser.usage());
      } else {
        out.err("Error: " + e.getMessage());
        if (e.showUsage()) {
          out.err(ArgParser.usage());
        }
      }
      return e.exitCode();
    }

    MockdbConfig config;
    try {
      config = MockdbConfig.fromEnv(env);
      HostGuard.require(config.host());
    } catch (HostRejectedException | IllegalArgumentException e) {
      out.err("Error: " + e.getMessage());
      return EXIT_FAILURE;
    }

    try (Connection connection = connector.open(config)) {
      return dispatch(invocation, connection, out);
    } catch (SQLException e) {
      out.err("Error: " + e.getMessage());
      return EXIT_FAILURE;
    }
  }

  private int dispatch(Invocation invocation, Connection connection, Output out) {
    return switch (invocation.command()) {
      case SCHEMA -> actions.schema(connection, out);
      case RESET -> actions.reset(connection, out);
      case SEED -> actions.seed(connection, out, invocation.scale(), invocation.seed());
      case UP -> actions.up(connection, out, invocation.scale(), invocation.seed());
    };
  }
}
