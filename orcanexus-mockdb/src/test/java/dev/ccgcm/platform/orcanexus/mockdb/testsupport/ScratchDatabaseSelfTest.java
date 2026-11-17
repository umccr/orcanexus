package dev.ccgcm.platform.orcanexus.mockdb.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

/** DB-backed harness self-test; enabled only with {@code -Pmockdb-it}. */
@EnabledIfSystemProperty(named = "mockdb.it", matches = "true")
class ScratchDatabaseSelfTest {

  @RegisterExtension static final ScratchDatabaseExtension DB = new ScratchDatabaseExtension();

  @Test
  void scratchDatabaseAnswersSelectOne() {
    assertThat(DB.dsl().fetchValue("select 1")).isEqualTo(1);
    assertThat(DB.dsl().fetchValue("select current_database()")).isEqualTo(DB.config().database());
  }
}
