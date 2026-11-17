package dev.ccgcm.platform.orcanexus.mockdb.config;

/** Thrown when the configured database host is not a local-only host. */
public final class HostRejectedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public HostRejectedException(String message) {
    super(message);
  }
}
