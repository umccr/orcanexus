package dev.ccgcm.platform.orcanexus.mockdb.config;

import java.util.Set;

/** Refuses any host other than the local loopback names. */
public final class HostGuard {

  private static final Set<String> ALLOWED = Set.of("localhost", "127.0.0.1", "::1");

  private HostGuard() {}

  /**
   * Requires the host to be exactly {@code localhost}, {@code 127.0.0.1} or {@code ::1}.
   *
   * @throws HostRejectedException naming the rejected host otherwise
   */
  public static void require(String host) {
    if (host == null || !ALLOWED.contains(host)) {
      throw new HostRejectedException(
          "Refusing to connect to host '"
              + host
              + "': the mock database tool only talks to localhost, 127.0.0.1 or ::1.");
    }
  }
}
