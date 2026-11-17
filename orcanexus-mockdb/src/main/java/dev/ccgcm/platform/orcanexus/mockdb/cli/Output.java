package dev.ccgcm.platform.orcanexus.mockdb.cli;

/** Sink for tool output, so logic never touches {@code System.out} directly. */
public interface Output {

  /** Writes one line to standard output. */
  void out(String line);

  /** Writes one line to standard error. */
  void err(String line);
}
