package dev.ccgcm.platform.orcanexus.mockdb.db;

import static org.assertj.core.api.Assertions.assertThat;

import dev.ccgcm.platform.orcanexus.jooq.Mart;
import dev.ccgcm.platform.orcanexus.mockdb.cli.Output;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.jooq.Field;
import org.jooq.Meta;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

/** DB-free tests for {@link SchemaService} (Requirements 5.3, 5.4, 5.6, 5.8, 5.9, 5.10). */
class SchemaServiceTest {

  private static final Pattern CREATE_SCHEMA =
      Pattern.compile("^create schema if not exists \\S+$", Pattern.CASE_INSENSITIVE);
  private static final Pattern CREATE_TABLE =
      Pattern.compile(
          "^create table if not exists \\S+\\s*\\(.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  private static final Pattern FORBIDDEN =
      Pattern.compile("\\b(drop|truncate|alter)\\b", Pattern.CASE_INSENSITIVE);

  private final SchemaService service = new SchemaService();

  /** Property 9, DB-free half: interpreted DDL matches the model column by column. */
  @Test
  void interpretedDdlMatchesModelColumns() {
    Meta meta = DSL.using(SQLDialect.POSTGRES).meta(service.ddlStatements().toArray(String[]::new));

    var interpreted =
        meta.getTables().stream()
            .filter(t -> Mart.MART.getName().equals(t.getSchema().getName()))
            .toList();
    assertThat(interpreted).hasSameSizeAs(Mart.MART.getTables());

    for (Table<?> expected : Mart.MART.getTables()) {
      var actual =
          interpreted.stream()
              .filter(t -> t.getName().equals(expected.getName()))
              .findFirst()
              .orElseThrow(() -> new AssertionError("missing table " + expected.getName()));
      assertThat(actual.fields()).hasSameSizeAs(expected.fields());
      for (Field<?> field : expected.fields()) {
        var got = actual.field(field.getName());
        var where = expected.getName() + "." + field.getName();
        assertThat(got).as(where).isNotNull();
        var want = field.getDataType();
        var have = got.getDataType();
        assertThat(have.getTypeName()).as(where + " type").isEqualTo(want.getTypeName());
        assertThat(have.length()).as(where + " length").isEqualTo(want.length());
        assertThat(have.precision()).as(where + " precision").isEqualTo(want.precision());
        assertThat(have.scale()).as(where + " scale").isEqualTo(want.scale());
        assertThat(have.nullable()).as(where + " nullable").isEqualTo(want.nullable());
      }
    }
  }

  /** Property 10 and 5.4: every statement is an idempotent create; nothing destructive. */
  @Test
  void statementsAreIdempotentCreatesOnly() {
    var statements = service.ddlStatements();
    assertThat(statements).isNotEmpty();
    assertThat(statements).allSatisfy(s -> assertThat(s).doesNotContainPattern(FORBIDDEN));

    var schemas = statements.stream().filter(s -> CREATE_SCHEMA.matcher(s.strip()).matches());
    var tables = statements.stream().filter(s -> CREATE_TABLE.matcher(s.strip()).matches());
    assertThat(schemas.count()).isEqualTo(1);
    assertThat(tables.count()).isEqualTo(Mart.MART.getTables().size());
    assertThat(statements).hasSize(1 + Mart.MART.getTables().size());
  }

  /** 5.3, 5.8: table count follows the model. */
  @Test
  void oneCreateTablePerModelTable() {
    var count =
        service.ddlStatements().stream()
            .filter(s -> s.toLowerCase(Locale.ROOT).startsWith("create table"))
            .count();
    assertThat(count).isEqualTo(Mart.MART.getTables().size());
  }

  @Test
  void ddlIsDeterministic() {
    assertThat(service.ddlStatements()).isEqualTo(service.ddlStatements());
    assertThat(new SchemaService().ddlStatements()).isEqualTo(service.ddlStatements());
  }

  @Test
  void failureOnNthStatementReportsAndReturnsOne() {
    var statements = service.ddlStatements();
    int failAt = 3;
    var executed = new ArrayList<String>();
    var tx = new ArrayList<String>();
    var conn = recording(failingConnection(failAt, executed), tx);
    var out = new RecordingOutput();

    int rc = service.schema(conn, out);

    assertThat(rc).isEqualTo(1);
    assertThat(executed).hasSize(failAt);
    assertThat(out.out).isEmpty();
    assertThat(out.err).anyMatch(l -> l.equals("Failing statement: " + statements.get(failAt - 1)));
    assertThat(out.err).anyMatch(l -> l.startsWith("Database error:") && l.contains("boom"));
    // Runs in one transaction: rolled back, never committed.
    assertThat(tx).contains("rollback").doesNotContain("commit");
  }

  @Test
  void failureOnFirstStatementIsReported() {
    var executed = new ArrayList<String>();
    var out = new RecordingOutput();

    int rc = service.schema(failingConnection(1, executed), out);

    assertThat(rc).isEqualTo(1);
    assertThat(out.err).anyMatch(l -> l.startsWith("Failing statement: "));
    assertThat(out.err).anyMatch(l -> l.contains("boom"));
  }

  @Test
  void successRunsAllStatementsInOneTransaction() {
    var executed = new ArrayList<String>();
    var tx = new ArrayList<String>();
    var out = new RecordingOutput();

    int rc = service.schema(recording(failingConnection(Integer.MAX_VALUE, executed), tx), out);

    assertThat(rc).isZero();
    assertThat(executed).hasSize(1 + Mart.MART.getTables().size());
    assertThat(tx).contains("commit").doesNotContain("rollback");
    assertThat(out.err).isEmpty();
    assertThat(out.out).singleElement().asString().contains(Mart.MART.getName());
  }

  private static Connection failingConnection(int failAt, List<String> executed) {
    var counter = new AtomicInteger();
    return new MockConnection(
        ctx -> {
          executed.add(ctx.sql());
          if (counter.incrementAndGet() == failAt) {
            throw new SQLException("boom");
          }
          return new MockResult[] {new MockResult(0)};
        });
  }

  /** Wraps a connection and records commit/rollback calls. */
  private static Connection recording(Connection delegate, List<String> calls) {
    return (Connection)
        Proxy.newProxyInstance(
            SchemaServiceTest.class.getClassLoader(),
            new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
              if (method.getName().equals("commit") || method.getName().equals("rollback")) {
                calls.add(method.getName());
              }
              try {
                return method.invoke(delegate, args);
              } catch (InvocationTargetException e) {
                throw e.getCause();
              }
            });
  }

  private static final class RecordingOutput implements Output {
    final List<String> out = new ArrayList<>();
    final List<String> err = new ArrayList<>();

    @Override
    public void out(String line) {
      out.add(line);
    }

    @Override
    public void err(String line) {
      err.add(line);
    }
  }
}
