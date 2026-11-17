package dev.ccgcm.platform.orcanexus.mockdb.cli;

/** Invalid command line, or a request for help. Carries the process exit code. */
public final class UsageException extends Exception {

  private static final long serialVersionUID = 1L;

  public static final int EXIT_OK = 0;
  public static final int EXIT_USAGE = 2;

  private final boolean showUsage;
  private final boolean help;

  /** A usage error (exit code 2). */
  public UsageException(String message, boolean showUsage) {
    this(message, showUsage, false);
  }

  private UsageException(String message, boolean showUsage, boolean help) {
    super(message);
    this.showUsage = showUsage;
    this.help = help;
  }

  /** The {@code --help} request: usage on standard output, exit code 0. */
  public static UsageException help() {
    return new UsageException("help requested", true, true);
  }

  /** Whether the usage text should be printed after the message. */
  public boolean showUsage() {
    return showUsage;
  }

  /** Whether this is a {@code --help} request rather than an error. */
  public boolean isHelp() {
    return help;
  }

  /** 0 for {@code --help}, 2 for every usage error. */
  public int exitCode() {
    return help ? EXIT_OK : EXIT_USAGE;
  }
}
