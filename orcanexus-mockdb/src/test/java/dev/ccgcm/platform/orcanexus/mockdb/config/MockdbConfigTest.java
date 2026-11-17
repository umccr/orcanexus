package dev.ccgcm.platform.orcanexus.mockdb.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Property 13: configuration ignores the tunnel variables. Validates: Requirements 4.8, 4.12. */
@DisplayName("Property 13: configuration ignores the tunnel variables")
class MockdbConfigTest {

  @Test
  void defaultsApplyWhenNothingIsSet() {
    var c = MockdbConfig.fromEnv(Map.of());
    assertThat(c.host()).isEqualTo("127.0.0.1");
    assertThat(c.port()).isEqualTo(5433);
    assertThat(c.database()).isEqualTo("orcanexus");
    assertThat(c.user()).isEqualTo("orcanexus");
    assertThat(c.password()).isEqualTo(MockdbConfig.DEFAULT_PASSWORD).isNotBlank();
  }

  @Test
  void mockdbVariablesOverrideDefaults() {
    var c =
        MockdbConfig.fromEnv(
            Map.of(
                "MOCKDB_HOST", "localhost",
                "MOCKDB_PORT", "6543",
                "MOCKDB_NAME", "other",
                "MOCKDB_USER", "bob",
                "MOCKDB_PASSWORD", "pw"));
    assertThat(c).isEqualTo(new MockdbConfig("localhost", 6543, "other", "bob", "pw"));
  }

  @Test
  void blankValuesFallBackToDefaults() {
    var c =
        MockdbConfig.fromEnv(
            Map.of(
                "MOCKDB_HOST", " ",
                "MOCKDB_PORT", "",
                "MOCKDB_NAME", "  ",
                "MOCKDB_USER", "",
                "MOCKDB_PASSWORD", " "));
    assertThat(c).isEqualTo(MockdbConfig.fromEnv(Map.of()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc", "0", "-1", "65536", "99999999999", "54.3"})
  void badPortRejectedNamingValue(String port) {
    assertThatThrownBy(() -> MockdbConfig.fromEnv(Map.of("MOCKDB_PORT", port)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("MOCKDB_PORT")
        .hasMessageContaining("'" + port + "'");
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "65535"})
  void portBoundariesAccepted(String port) {
    assertThat(MockdbConfig.fromEnv(Map.of("MOCKDB_PORT", port)).port())
        .isEqualTo(Integer.parseInt(port));
  }

  @Test
  void tunnelVariablesHaveNoEffect() {
    var junk = new HashMap<String, String>();
    junk.put("DB_HOST", "aurora.cluster-x.ap-southeast-2.rds.amazonaws.com");
    junk.put("DB_PORT", "not-a-port");
    junk.put("DB_NAME", "prod");
    junk.put("DB_USER", "admin");
    junk.put("DB_PASS", "secret");
    assertThat(MockdbConfig.fromEnv(junk)).isEqualTo(MockdbConfig.fromEnv(Map.of()));

    var withMock = new HashMap<>(junk);
    withMock.put("MOCKDB_HOST", "localhost");
    withMock.put("MOCKDB_PORT", "7000");
    assertThat(MockdbConfig.fromEnv(withMock))
        .isEqualTo(MockdbConfig.fromEnv(Map.of("MOCKDB_HOST", "localhost", "MOCKDB_PORT", "7000")));
  }

  @Test
  void toStringOmitsPassword() {
    var c = new MockdbConfig("localhost", 5433, "db", "u", "hunter2");
    assertThat(c.toString()).contains("localhost", "5433", "db", "u").doesNotContain("hunter2");
  }
}
