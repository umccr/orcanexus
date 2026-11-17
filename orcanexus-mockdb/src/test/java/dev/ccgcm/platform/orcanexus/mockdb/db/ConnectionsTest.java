package dev.ccgcm.platform.orcanexus.mockdb.db;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccgcm.platform.orcanexus.mockdb.config.MockdbConfig;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Pure helpers of {@link Connections}. Validates: Requirements 4.10, 4.12. */
class ConnectionsTest {

  private static final MockdbConfig CONFIG =
      new MockdbConfig("127.0.0.1", 5433, "orcanexus", "orcanexus", "pw");

  @Test
  void jdbcUrlPlainHost() {
    assertThat(Connections.jdbcUrl(CONFIG)).isEqualTo("jdbc:postgresql://127.0.0.1:5433/orcanexus");
  }

  @Test
  void jdbcUrlBracketsIpv6Loopback() {
    var c = new MockdbConfig("::1", 5433, "orcanexus", "u", "p");
    assertThat(Connections.jdbcUrl(c)).isEqualTo("jdbc:postgresql://[::1]:5433/orcanexus");
  }

  @Test
  void jdbcUrlHasNoCredentials() {
    assertThat(Connections.jdbcUrl(CONFIG)).doesNotContain("pw");
  }

  @Test
  void propertiesCarryTimeoutsAndBatchRewrite() {
    var p = Connections.properties(CONFIG);
    assertThat(p.getProperty("connectTimeout")).isEqualTo("10");
    assertThat(p.getProperty("loginTimeout")).isEqualTo("10");
    assertThat(p.getProperty("reWriteBatchedInserts")).isEqualTo("true");
    assertThat(p.getProperty("user")).isEqualTo("orcanexus");
    assertThat(p.getProperty("password")).isEqualTo("pw");
  }

  @ParameterizedTest
  @ValueSource(strings = {"08001", "08006", "08004"})
  void unreachableStatesNameHostPortAndComposeHint(String state) {
    var t = Connections.translate(CONFIG, new SQLException("boom", state));
    assertThat(t.getMessage()).contains("127.0.0.1:5433").contains("Compose");
    assertThat(t.getCause()).isNotNull();
  }

  @Test
  void connectionRefusedMessageIsTranslated() {
    var t = Connections.translate(CONFIG, new SQLException("Connection refused (x)"));
    assertThat(t.getMessage()).contains("127.0.0.1:5433").contains("nexus_docker_up");
  }

  @Test
  void badPasswordIsTranslatedWithoutLeakingPassword() {
    var t = Connections.translate(CONFIG, new SQLException("auth", "28P01"));
    assertThat(t.getMessage()).contains("orcanexus").contains("MOCKDB_PASSWORD");
    assertThat(t.getMessage()).doesNotContain("pw");
  }

  @Test
  void unknownFailurePassesThroughUnchanged() {
    var original = new SQLException("syntax", "42601");
    assertThat((Object) Connections.translate(CONFIG, original)).isSameAs(original);
    var noMessage = new SQLException((String) null);
    assertThat((Object) Connections.translate(CONFIG, noMessage)).isSameAs(noMessage);
  }
}
