package dev.ccgcm.platform.orcanexus.mockdb.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Pure command-line parser. No CLI framework, no I/O. */
public final class ArgParser {

  private static final String SCALE = "--scale";
  private static final String SEED = "--seed";
  private static final String HELP = "--help";

  private ArgParser() {}

  /**
   * Parses the arguments.
   *
   * @throws UsageException for {@code --help} (exit 0) and for every invalid command line (exit 2)
   */
  public static Invocation parse(String[] args) throws UsageException {
    List<String> list = List.of(args);
    if (list.contains(HELP) || list.contains("-h")) {
      throw UsageException.help();
    }
    if (list.isEmpty()) {
      throw new UsageException("missing subcommand", true);
    }
    String first = list.get(0);
    Optional<Command> found = Command.fromName(first);
    if (found.isEmpty()) {
      String what = first.startsWith("-") ? "unknown option" : "unknown subcommand";
      throw new UsageException(what + ": " + first, true);
    }
    Command command = found.get();

    Integer scale = null;
    Integer seed = null;
    List<String> rest = new ArrayList<>(list.subList(1, list.size()));
    int i = 0;
    while (i < rest.size()) {
      String option = rest.get(i);
      boolean isScale = SCALE.equals(option);
      boolean isSeed = SEED.equals(option);
      if (!isScale && !isSeed) {
        String what = option.startsWith("-") ? "unknown option" : "unexpected argument";
        throw new UsageException(what + ": " + option, true);
      }
      boolean accepted = isScale ? command.acceptsScale() : command.acceptsSeed();
      if (!accepted) {
        throw new UsageException(
            "option " + option + " is not supported by '" + command.cliName() + "'", true);
      }
      if ((isScale && scale != null) || (isSeed && seed != null)) {
        throw new UsageException("duplicate option: " + option, true);
      }
      if (i + 1 >= rest.size() || rest.get(i + 1).startsWith("--")) {
        throw new UsageException("option " + option + " requires a value", true);
      }
      String value = rest.get(i + 1);
      if (isScale) {
        scale = parseInt(option, value, Invocation.MIN_SCALE, Invocation.MAX_SCALE);
      } else {
        seed = parseInt(option, value, 0, Integer.MAX_VALUE);
      }
      i += 2;
    }
    return new Invocation(
        command,
        scale == null ? Invocation.DEFAULT_SCALE : scale,
        seed == null ? Invocation.DEFAULT_SEED : seed);
  }

  private static int parseInt(String option, String value, int min, int max) throws UsageException {
    String range =
        max == Integer.MAX_VALUE
            ? "an integer of " + min + " or more"
            : "an integer from " + min + " to " + max;
    int parsed;
    try {
      parsed = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new UsageException(
          "invalid value for " + option + ": '" + value + "' (must be " + range + ")", true);
    }
    if (parsed < min || parsed > max) {
      throw new UsageException(
          "invalid value for " + option + ": '" + value + "' (must be " + range + ")", true);
    }
    return parsed;
  }

  /** The usage text: every subcommand, every option and the environment variables. */
  public static String usage() {
    return String.join(
        System.lineSeparator(),
        "Usage: orcanexus-mockdb <subcommand> [options]",
        "",
        "Subcommands:",
        "  schema   Create the schema and tables if they do not exist",
        "  seed     Insert synthetic rows (tables must be empty)",
        "  reset    Drop the schema and recreate it empty",
        "  up       reset, then schema, then seed",
        "",
        "Options (seed and up only):",
        "  --scale N   Number of libraries, 1 to 100000 (default 100)",
        "  --seed N    Random seed, 0 or more (default 42)",
        "  --help      Print this text and exit 0",
        "",
        "Environment variables (defaults in brackets):",
        "  MOCKDB_HOST      [127.0.0.1]",
        "  MOCKDB_PORT      [5433]",
        "  MOCKDB_NAME      [orcanexus]",
        "  MOCKDB_USER      [orcanexus]",
        "  MOCKDB_PASSWORD  [local-only default]",
        "",
        "Exit codes: 0 success or --help, 1 runtime failure, 2 usage error.");
  }
}
