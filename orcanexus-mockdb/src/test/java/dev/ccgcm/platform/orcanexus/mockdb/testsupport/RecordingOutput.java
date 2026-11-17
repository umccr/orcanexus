package dev.ccgcm.platform.orcanexus.mockdb.testsupport;

import dev.ccgcm.platform.orcanexus.mockdb.cli.Output;
import java.util.ArrayList;
import java.util.List;

/** Collects tool output lines for assertions. */
public final class RecordingOutput implements Output {

  private final List<String> out = new ArrayList<>();
  private final List<String> err = new ArrayList<>();

  @Override
  public void out(String line) {
    out.add(line);
  }

  @Override
  public void err(String line) {
    err.add(line);
  }

  public String allOut() {
    return String.join("\n", out);
  }

  public String allErr() {
    return String.join("\n", err);
  }
}
