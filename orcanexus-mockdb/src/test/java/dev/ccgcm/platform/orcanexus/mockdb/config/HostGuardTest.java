package dev.ccgcm.platform.orcanexus.mockdb.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Property 14: host guard. Validates: Requirements 9.6, 9.7. */
@DisplayName("Property 14: host guard")
class HostGuardTest {

  @ParameterizedTest
  @ValueSource(strings = {"localhost", "127.0.0.1", "::1"})
  void allowedHostsPass(String host) {
    assertThatCode(() -> HostGuard.require(host)).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "127.0.0.2",
        "0.0.0.0",
        "example.com",
        "orca-dev.cluster-abc123.ap-southeast-2.rds.amazonaws.com",
        "LOCALHOST",
        "Localhost",
        "localhost.",
        " 127.0.0.1",
        "127.0.0.1 ",
        "[::1]",
        "::2",
        " "
      })
  void everyOtherHostRejectedNamingIt(String host) {
    assertThatThrownBy(() -> HostGuard.require(host))
        .isInstanceOf(HostRejectedException.class)
        .hasMessageContaining("'" + host + "'");
  }

  @Test
  void caseVariantsAreRejectedByDesign() {
    assertThat(HostGuard.class).isNotNull();
    assertThatThrownBy(() -> HostGuard.require("LOCALHOST"))
        .isInstanceOf(HostRejectedException.class);
  }
}
