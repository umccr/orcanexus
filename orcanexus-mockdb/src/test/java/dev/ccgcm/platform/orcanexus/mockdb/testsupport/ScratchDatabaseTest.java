package dev.ccgcm.platform.orcanexus.mockdb.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.ccgcm.platform.orcanexus.mockdb.config.HostRejectedException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Runs by default: harness rules that need no database. Validates: Requirements 13.2, 13.3. */
class ScratchDatabaseTest {

  @Test
  void defaultNameWhenUnset() {
    assertThat(ScratchDatabase.scratchName(Map.of())).isEqualTo("orcanexus_it");
    assertThat(ScratchDatabase.scratchName(Map.of("MOCKDB_IT_NAME", " ")))
        .isEqualTo("orcanexus_it");
  }

  @Test
  void acceptsConfiguredName() {
    assertThat(ScratchDatabase.scratchName(Map.of("MOCKDB_IT_NAME", "it_2"))).isEqualTo("it_2");
  }

  @ParameterizedTest
  @ValueSource(strings = {"Upper", "a-b", "a b", "a\"; drop database x; --", "é"})
  void rejectsBadIdentifiers(String name) {
    assertThatThrownBy(() -> ScratchDatabase.requireIdentifier(name))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void refusesNonLocalHostBeforeConnecting() {
    var env = Map.of("MOCKDB_HOST", "db.example.com");
    assertThatThrownBy(
            () ->
                ScratchDatabase.create(
                    env,
                    cfg -> {
                      throw new AssertionError("must not connect");
                    }))
        .isInstanceOf(HostRejectedException.class)
        .hasMessageContaining("db.example.com");
  }

  @Test
  void unreachableServerNamesHostAndPort() {
    var env = Map.of("MOCKDB_PORT", "5444");
    assertThatThrownBy(
            () ->
                ScratchDatabase.create(
                    env,
                    cfg -> {
                      throw new java.sql.SQLException("refused");
                    }))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("127.0.0.1:5444")
        .hasMessageContaining("nexus_docker_up");
  }
}
