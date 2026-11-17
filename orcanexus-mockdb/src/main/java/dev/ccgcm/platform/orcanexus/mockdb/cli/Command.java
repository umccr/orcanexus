package dev.ccgcm.platform.orcanexus.mockdb.cli;

import java.util.Locale;
import java.util.Optional;

/** Mockdb_Tool subcommands and the options each one accepts. */
public enum Command {
  SCHEMA(false),
  SEED(true),
  RESET(false),
  UP(true);

  private final boolean acceptsOptions;

  Command(boolean acceptsOptions) {
    this.acceptsOptions = acceptsOptions;
  }

  /** The subcommand as typed on the command line. */
  public String cliName() {
    return name().toLowerCase(Locale.ROOT);
  }

  /** Whether {@code --scale} is allowed. */
  public boolean acceptsScale() {
    return acceptsOptions;
  }

  /** Whether {@code --seed} is allowed. */
  public boolean acceptsSeed() {
    return acceptsOptions;
  }

  /** Looks up a subcommand by its exact lower-case name. */
  public static Optional<Command> fromName(String name) {
    for (Command command : values()) {
      if (command.cliName().equals(name)) {
        return Optional.of(command);
      }
    }
    return Optional.empty();
  }
}
