package dev.ccgcm.platform.orcanexus.mockdb.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Property 12: Argument parsing. Validates: Requirements 4.1, 4.3-4.7, 6.8. */
@DisplayName("Property 12: argument parsing")
class ArgParserTest {

  private static UsageException rejected(String... args) {
    var thrown = new UsageException[1];
    assertThatThrownBy(() -> ArgParser.parse(args))
        .isInstanceOfSatisfying(UsageException.class, e -> thrown[0] = e);
    return thrown[0];
  }

  @ParameterizedTest
  @ValueSource(strings = {"schema", "seed", "reset", "up"})
  void eachSubcommandParsesWithDefaults(String name) throws Exception {
    var inv = ArgParser.parse(new String[] {name});
    assertThat(inv.command().cliName()).isEqualTo(name);
    assertThat(inv.scale()).isEqualTo(100);
    assertThat(inv.seed()).isEqualTo(42);
  }

  @ParameterizedTest
  @ValueSource(strings = {"seed", "up"})
  void scaleAndSeedAcceptedInEitherOrder(String name) throws Exception {
    var a = ArgParser.parse(new String[] {name, "--scale", "7", "--seed", "9"});
    var b = ArgParser.parse(new String[] {name, "--seed", "9", "--scale", "7"});
    assertThat(a).isEqualTo(b);
    assertThat(a.scale()).isEqualTo(7);
    assertThat(a.seed()).isEqualTo(9);
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "100000", "50"})
  void scaleBoundariesAccepted(String value) throws Exception {
    var inv = ArgParser.parse(new String[] {"seed", "--scale", value});
    assertThat(inv.scale()).isEqualTo(Integer.parseInt(value));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "1", "2147483647"})
  void seedBoundariesAccepted(String value) throws Exception {
    var inv = ArgParser.parse(new String[] {"up", "--seed", value});
    assertThat(inv.seed()).isEqualTo(Integer.parseInt(value));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "100001", "-1", "abc", "", "1.5", "99999999999"})
  void invalidScaleNamesOptionAndValue(String value) {
    var e = rejected("seed", "--scale", value);
    assertThat(e.exitCode()).isEqualTo(2);
    assertThat(e.getMessage()).contains("--scale").contains("'" + value + "'");
  }

  @ParameterizedTest
  @ValueSource(strings = {"-1", "abc", "", "2147483648"})
  void invalidSeedNamesOptionAndValue(String value) {
    var e = rejected("up", "--seed", value);
    assertThat(e.exitCode()).isEqualTo(2);
    assertThat(e.getMessage()).contains("--seed").contains("'" + value + "'");
  }

  @ParameterizedTest
  @CsvSource({"--scale", "--seed"})
  void duplicateOptionRejected(String option) {
    var e = rejected("seed", option, "5", option, "6");
    assertThat(e.getMessage()).contains("duplicate").contains(option);
  }

  @ParameterizedTest
  @CsvSource({"--scale", "--seed"})
  void missingValueRejected(String option) {
    assertThat(rejected("seed", option).getMessage()).contains(option).contains("value");
    assertThat(
            rejected("seed", option, "--seed".equals(option) ? "--scale" : "--seed", "3")
                .getMessage())
        .contains(option);
  }

  @ParameterizedTest
  @CsvSource({"schema,--scale", "schema,--seed", "reset,--scale", "reset,--seed"})
  void optionsOnSchemaAndResetRejected(String command, String option) {
    var e = rejected(command, option, "5");
    assertThat(e.exitCode()).isEqualTo(2);
    assertThat(e.getMessage()).contains(option).contains(command);
  }

  @Test
  void unknownSubcommandIsNamed() {
    var e = rejected("frobnicate");
    assertThat(e.exitCode()).isEqualTo(2);
    assertThat(e.getMessage()).contains("unknown subcommand").contains("frobnicate");
    assertThat(e.showUsage()).isTrue();
  }

  @Test
  void subcommandIsCaseSensitive() {
    assertThat(rejected("SEED").getMessage()).contains("SEED");
  }

  @Test
  void unknownOptionAndStrayArgumentRejected() {
    assertThat(rejected("seed", "--bogus").getMessage()).contains("unknown option: --bogus");
    assertThat(rejected("seed", "extra").getMessage()).contains("extra");
    assertThat(rejected("--bogus").getMessage()).contains("--bogus");
  }

  @ParameterizedTest
  @ValueSource(strings = {"--help", "-h"})
  void helpExitsZeroWithUsage(String flag) {
    var e = rejected(flag);
    assertThat(e.isHelp()).isTrue();
    assertThat(e.exitCode()).isZero();
    e = rejected("seed", "--scale", "5", flag);
    assertThat(e.exitCode()).isZero();
  }

  @Test
  void noSubcommandIsNonZeroUsageError() {
    var e = rejected();
    assertThat(e.isHelp()).isFalse();
    assertThat(e.exitCode()).isNotZero();
    assertThat(e.getMessage()).contains("missing subcommand");
  }

  @Test
  void usageListsEverySubcommandAndOption() {
    assertThat(ArgParser.usage())
        .contains("schema", "seed", "reset", "up", "--scale", "--seed", "--help", "MOCKDB_HOST");
  }
}
