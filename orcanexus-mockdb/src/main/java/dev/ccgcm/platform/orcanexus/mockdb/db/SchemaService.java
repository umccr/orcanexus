package dev.ccgcm.platform.orcanexus.mockdb.db;

import dev.ccgcm.platform.orcanexus.jooq.Mart;
import dev.ccgcm.platform.orcanexus.mockdb.cli.Output;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jooq.DDLExportConfiguration;
import org.jooq.DDLFlag;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

/**
 * Creates and resets the local schema from the jOOQ model. All statements are jOOQ {@link Query}
 * objects; no SQL is concatenated by hand.
 */
public final class SchemaService {

  private static final int OK = 0;
  private static final int FAILED = 1;

  private final DSLContext renderer = DSL.using(SQLDialect.POSTGRES);

  /** Builds the idempotent DDL (schema and tables only) for the model. */
  public List<Query> ddlQueries() {
    var cfg =
        new DDLExportConfiguration()
            .flags(DDLFlag.SCHEMA, DDLFlag.TABLE)
            .createSchemaIfNotExists(true)
            .createTableIfNotExists(true)
            .inlinePrimaryKeyConstraints(true);
    return new ArrayList<>(List.of(renderer.ddl(Mart.MART, cfg).queries()));
  }

  /** Returns the DDL statements rendered as SQL text, for tests and diagnostics. */
  public List<String> ddlStatements() {
    return ddlQueries().stream().map(q -> q.getSQL(ParamType.INLINED)).toList();
  }

  /** Runs the DDL in one transaction. Existing objects and rows are left unchanged. */
  public int schema(Connection connection, Output output) {
    var dsl = DSL.using(connection, SQLDialect.POSTGRES);
    var queries = ddlQueries();
    int rc = run(dsl, queries, false, output);
    if (rc == OK) {
      output.out(
          "schema: created or verified "
              + Mart.MART.getTables().size()
              + " tables in schema "
              + Mart.MART.getName());
    }
    return rc;
  }

  /** Drops the model's schema (cascade) and recreates it, in one transaction. */
  public int reset(Connection connection, Output output) {
    var dsl = DSL.using(connection, SQLDialect.POSTGRES);
    var queries = ddlQueries();
    int rc = run(dsl, queries, true, output);
    if (rc == OK) {
      output.out(
          "reset: dropped and recreated schema "
              + Mart.MART.getName()
              + " with "
              + Mart.MART.getTables().size()
              + " tables");
    }
    return rc;
  }

  private static int run(DSLContext dsl, List<Query> queries, boolean drop, Output output) {
    var current = new AtomicReference<Query>();
    try {
      dsl.transaction(
          cfg -> {
            var tx = DSL.using(cfg);
            if (drop) {
              var dropQuery = tx.dropSchemaIfExists(Mart.MART).cascade();
              current.set(dropQuery);
              dropQuery.execute();
            }
            for (Query query : queries) {
              current.set(query);
              tx.attach(query);
              query.execute();
            }
          });
      return OK;
    } catch (DataAccessException e) {
      var failing = current.get();
      if (failing != null) {
        output.err("Failing statement: " + failing.getSQL(ParamType.INLINED));
      }
      output.err("Database error: " + e.getMessage());
      return FAILED;
    }
  }
}
