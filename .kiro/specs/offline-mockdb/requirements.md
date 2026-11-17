# Requirements Document

## Introduction

Development today is remote-only ([ADR 0005](../../../docs/adr/0005-remote-only-dev-with-iam-auth.md)):
the app reaches the dev Aurora PostgreSQL through an SSM tunnel with IAM database auth. That
path is slow and the tunnel drops. This feature adds an
offline path next to it: a local PostgreSQL in Docker Compose, filled with purely synthetic mock
data, selected through a new Spring profile `mockdb`.

The schema is derived from the committed jOOQ model (schema `mart`, seven flat relations:
`lims`, `fastq`, `bam`, `vcf`, `workflow`, `workflow_output`, `workflow_cost_report`). A new
dev-only Maven module, `orcanexus-mockdb`, provides a small CLI that creates the schema and
seeds deterministic data. The default profile stays the remote dev profile, now named `tunnel` (see Requirement 14), and the application jar, the
domain, the REST adapter and the Aurora adapter are unchanged in behaviour.

## Glossary

- **Mockdb_Profile**: The Spring profile `mockdb`, activated explicitly, that connects the
  Application to the Local_Database.
- **Tunnel_Profile**: The Spring profile `tunnel`, the default profile, that connects the Application to the remote dev Aurora PostgreSQL through the SSM tunnel on 127.0.0.1 with IAM database auth. It replaces the retired profile `local`.
- **Application**: The OrcaNexus Spring Boot service built from `orcanexus-application`.
- **Local_Database**: A PostgreSQL instance run by Docker Compose on the developer machine.
- **Mockdb_Env_Vars**: The environment variables `MOCKDB_HOST` (default `127.0.0.1`), `MOCKDB_PORT` (default `5433`), `MOCKDB_NAME` (default `orcanexus`), `MOCKDB_USER` (default `orcanexus`) and `MOCKDB_PASSWORD` (a documented local-only default value). They are separate from the tunnel variables.
- **Tunnel_Env_Vars**: The environment variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` and `DB_PASS`, used by the Tunnel_Profile and not used by the mockdb path.
- **Compose_Stack**: The Docker Compose definition that runs the Local_Database.
- **Mockdb_Tool**: The CLI in the dev-only Maven module `orcanexus-mockdb`.
- **Schema_Command**: The Mockdb_Tool subcommand `schema`.
- **Seed_Command**: The Mockdb_Tool subcommand `seed`.
- **Reset_Command**: The Mockdb_Tool subcommand `reset`.
- **Up_Command**: The Mockdb_Tool subcommand `up`, equivalent to Reset_Command, then Schema_Command, then Seed_Command.
- **Jooq_Model**: The committed generated jOOQ model in `orcanexus-jooq`, schema `mart`.
- **Mart_Relations**: The seven relations of the Jooq_Model.
- **Key_Pools**: Curated sets of shared identifiers (subjects, samples, libraries, sequencing runs, portal run ids) from which every seeded table draws its key values.
- **Relationship_Spec**: A small per-table specification, maintained in the Mockdb_Tool, that states which columns draw from which Key_Pool, because the Jooq_Model declares no keys.
- **Scale**: The number of libraries to seed, set by `--scale N`; the default is 100.
- **Seed_Value**: The integer that initialises the random generator, set by `--seed`.
- **Dx_Script**: The repository helper script `dx.sh`.
- **Quality_Gates**: `mvn -DskipTests compile`, `mvn spotless:apply` and `mvn verify` (tests, Spotless, Checkstyle, SpotBugs).

## Requirements

### Requirement 1: Local database via Docker Compose

**User Story:** As a developer, I want a local PostgreSQL started with one command, so that I
can work when the tunnel is slow or down.

#### Acceptance Criteria

1. THE Compose_Stack SHALL run a PostgreSQL image pinned to an explicit major.minor version tag (for example, 18.6), and THE Compose_Stack SHALL NOT use the latest tag or a major-only tag.
2. THE Compose_Stack SHALL store database files in a named volume, and SHALL mount that volume at the data location the pinned image version expects (for PostgreSQL 18 and later, `/var/lib/postgresql`), so that a restart retains the data.
3. THE Compose_Stack SHALL define a healthcheck that reports healthy only when the Local_Database accepts connections, with a check interval of 5 seconds, a per-check timeout of 5 seconds, and a maximum of 10 consecutive failures before the container is reported unhealthy.
4. THE Compose_Stack SHALL publish the Local_Database on a host port that is overridable through the environment variable `MOCKDB_PORT`.
5. THE Compose_Stack SHALL default the host port to 5433, a value other than 5432, so that the Local_Database does not collide with an active SSM tunnel.
6. THE Compose_Stack SHALL take the database name, user and password from `MOCKDB_NAME`, `MOCKDB_USER` and `MOCKDB_PASSWORD`, with the local-only defaults defined in the Glossary.
7. WHEN the Compose_Stack is stopped and started again without volume removal, THE Local_Database SHALL retain all previously seeded data, with row counts in every seeded table identical before and after the restart.
8. IF the configured host port is already in use on the host, THEN THE Compose_Stack SHALL fail to start the Local_Database, report a port-binding error to the developer, and leave the named volume and its data unchanged.
9. WHEN the Compose_Stack is started on a host where the named volume does not yet exist, THE Compose_Stack SHALL create an empty Local_Database using the configured database name, user and password, and report healthy within 60 seconds.

### Requirement 2: Mockdb Spring profile

**User Story:** As a developer, I want to start the Application against the Local_Database by
selecting one profile, so that I need no AWS session.

#### Acceptance Criteria

1. WHERE the Mockdb_Profile is active, THE Application SHALL connect using a plain `jdbc:postgresql` URL and the `org.postgresql.Driver` class.
2. WHERE the Mockdb_Profile is active, THE Application SHALL authenticate with a static user and password taken from `MOCKDB_USER` and `MOCKDB_PASSWORD`, and SHALL use the local-only defaults from the Glossary when those variables are unset.
3. WHERE the Mockdb_Profile is active, THE Application SHALL complete startup, with the application context reporting a started state within 60 seconds, when no AWS credentials, no AWS session and no SSM tunnel are present.
4. WHERE the Mockdb_Profile is active, THE Application SHALL read the host, port and database name from `MOCKDB_HOST`, `MOCKDB_PORT` and `MOCKDB_NAME`, and SHALL default to the host, port and database name published by the Compose_Stack when those variables are unset.
5. WHEN no Spring profile is selected, THE Application SHALL activate the Tunnel_Profile.
6. WHERE the Mockdb_Profile is active, THE Application SHALL use schema `mart` without requiring `DB_SCHEMA_INPUT` or `DB_SCHEMA_OUTPUT` to be set, and SHALL apply no schema rewrite at query-render time (the generated and physical schema names are identical).
7. WHILE the Tunnel_Profile or any other non-mockdb profile is active, THE Application SHALL use datasource settings identical to those in effect before this feature, with no change in driver class, URL format, authentication method or schema mappings.
8. THE Application SHALL keep the AWS Advanced JDBC Wrapper dependency available on the classpath for non-mockdb profiles.
9. WHERE the Mockdb_Profile is active, THE Application SHALL set `orcanexus.system.environment` to `mockdb`.
10. THE Application SHALL define the Mockdb_Profile settings as Spring configuration, and SHALL NOT add a new custom `orcanexus.*` property for any setting that Spring Boot already provides.
11. IF the Local_Database is unreachable or rejects the configured credentials while the Mockdb_Profile is active, THEN THE Application SHALL fail startup, exit with a non-zero status, and report an error indicating the database connection failure, without falling back to AWS IAM authentication.
12. WHERE the Mockdb_Profile is active, THE Application SHALL ignore the Tunnel_Env_Vars, so that values of `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` and `DB_PASS` set for the tunnel have no effect on the connection target, and SHALL NOT use AWS IAM credentials for the connection.

### Requirement 3: Dev-only module boundary

**User Story:** As a maintainer, I want the mock tooling kept out of the runtime, so that the
hexagonal rules and the application jar stay clean.

#### Acceptance Criteria

1. THE Build SHALL contain a Maven module named `orcanexus-mockdb` listed in the `modules` section of the parent `pom.xml`.
2. THE `orcanexus-mockdb` module SHALL declare compile-scope or runtime-scope dependencies only on `orcanexus-jooq`, the PostgreSQL JDBC driver and Datafaker (plus test-scope dependencies), and SHALL declare no dependency on `orcanexus-domain`, `orcanexus-rest`, `orcanexus-aurora` or `orcanexus-application`.
3. THE `orcanexus-application`, `orcanexus-rest`, `orcanexus-aurora` and `orcanexus-domain` modules SHALL NOT declare a direct or transitive dependency on `orcanexus-mockdb`.
4. WHEN `mvn -DskipTests package` completes, THE Application jar SHALL contain zero classes from the `orcanexus-mockdb` module and zero classes from the `net.datafaker` package.
5. THE `orcanexus-mockdb` module SHALL declare the PostgreSQL JDBC driver and Datafaker each with an exact version (no version ranges, no `LATEST`, no `RELEASE`), or reuse the version managed by the Spring Boot parent where one exists.
6. THE `orcanexus-domain` module SHALL declare zero dependencies on Spring, jOOQ, the PostgreSQL JDBC driver, Datafaker or any other module.
7. WHEN `mvn verify` runs, THE `orcanexus-mockdb` module SHALL complete Spotless, Checkstyle and SpotBugs checks with zero violations and the build SHALL exit with status 0.
8. IF `mvn verify` reports a Spotless, Checkstyle or SpotBugs violation in `orcanexus-mockdb`, THEN THE Build SHALL fail with a non-zero exit status identifying the violating module.
9. THE `orcanexus-mockdb` module SHALL NOT add, modify or delete any file under the Jooq_Model package (`dev.ccgcm.platform.orcanexus.jooq`).

### Requirement 4: CLI surface

**User Story:** As a developer, I want a small set of predictable commands, so that I can
rebuild my local data quickly.

#### Acceptance Criteria

1. THE Mockdb_Tool SHALL provide the subcommands `schema`, `seed`, `reset` and `up`.
2. THE Mockdb_Tool SHALL parse its arguments without a CLI framework.
3. WHEN the Mockdb_Tool is started with no subcommand or with `--help`, THE Mockdb_Tool SHALL print the usage text listing every subcommand and every option, and SHALL exit with status 0 for `--help` and a non-zero status for no subcommand.
4. IF the subcommand is unknown, THEN THE Mockdb_Tool SHALL print the usage text listing the subcommands and options, SHALL print a message naming the unknown subcommand, and SHALL exit with a non-zero status.
5. IF an option value is invalid (not an integer, or `--scale` outside 1 to 100000, or `--seed` less than 0), THEN THE Mockdb_Tool SHALL print a message naming the option and the rejected value, SHALL exit with a non-zero status, and SHALL NOT connect to or modify the Local_Database.
6. THE Mockdb_Tool SHALL accept `--scale N` and `--seed N` on the `seed` and `up` subcommands.
7. IF `--scale` or `--seed` is supplied to the `schema` or `reset` subcommand, THEN THE Mockdb_Tool SHALL print a message naming the unsupported option, SHALL exit with a non-zero status, and SHALL NOT modify the Local_Database.
8. THE Mockdb_Tool SHALL read the connection host, port, database name, user and password from the Mockdb_Env_Vars, using the default value from the Glossary for each variable when it is unset.
9. WHEN a subcommand completes successfully, THE Mockdb_Tool SHALL exit with status 0.
10. IF the Local_Database is unreachable within 10 seconds of the connection attempt, THEN THE Mockdb_Tool SHALL print a message that names the host and port and suggests starting the Compose_Stack, and SHALL exit with a non-zero status.
11. WHEN the Mockdb_Tool completes `schema`, `seed`, `reset` or `up`, THE Mockdb_Tool SHALL print a summary naming the action taken, including the row count per table for `seed` and `up`.
12. THE Mockdb_Tool SHALL ignore the Tunnel_Env_Vars, so that values of `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` and `DB_PASS` set for the tunnel have no effect on the connection target, and SHALL NOT use AWS IAM credentials for any connection.

### Requirement 5: Schema derived from the jOOQ model

**User Story:** As a developer, I want the local DDL generated from the committed model, so
that local tables never drift from the code that queries them.

#### Acceptance Criteria

1. WHEN the Schema_Command runs against a Local_Database that does not contain schema `mart`, THE Mockdb_Tool SHALL create schema `mart` and all seven Mart_Relations in the Local_Database using DDL produced by jOOQ `DSLContext.ddl` from the Jooq_Model, and SHALL exit with status 0.
2. WHEN the Schema_Command runs, THE Mockdb_Tool SHALL create each Mart_Relation as a physical table, because the real warehouse objects are views and a table is the portable local equivalent.
3. WHEN the Schema_Command runs, THE Mockdb_Tool SHALL create each column with the name, type, length, precision, scale and nullability declared in the Jooq_Model.
4. WHEN the Schema_Command runs against a Local_Database that already contains schema `mart` and all seven Mart_Relations, THE Mockdb_Tool SHALL leave existing objects and their rows unchanged, SHALL execute no DROP, TRUNCATE or ALTER statement, and SHALL exit with status 0. This criterion applies to the Schema_Command only; the Reset_Command drops schema `mart` (Requirement 9).
5. IF the Local_Database already contains schema `mart` but lacks one or more of the seven Mart_Relations, THEN THE Mockdb_Tool SHALL create only the missing Mart_Relations, SHALL leave the existing ones unchanged, and SHALL exit with status 0.
6. IF the jOOQ-generated DDL for any Mart_Relation is rejected by PostgreSQL, THEN THE Mockdb_Tool SHALL print the failing statement and the database error to standard error, SHALL roll back all changes made by that Schema_Command run so that no partial schema remains, and SHALL exit with a non-zero status.
7. IF the Local_Database is unreachable within 10 seconds of the Schema_Command starting, THEN THE Mockdb_Tool SHALL print an error indicating the connection failure to standard error, SHALL create no objects, and SHALL exit with a non-zero status.
8. WHEN the Jooq_Model is regenerated with an added or removed relation or column, THE Schema_Command SHALL reflect the change without a source change in the Mockdb_Tool.
9. FOR ALL Mart_Relations, the column names, column types and nullability read from the Local_Database catalog after the Schema_Command SHALL equal those declared in the Jooq_Model (schema fidelity property).
10. THE Schema_Command SHALL achieve the idempotent behaviour of criteria 4 and 5 through IF NOT EXISTS style DDL (the jOOQ DDL export flags or an equivalent), without a pre-check of the catalog.

### Requirement 6: Synthetic, deterministic data

**User Story:** As a developer, I want repeatable mock data, so that a bug seen by one
developer can be reproduced by another.

#### Acceptance Criteria

1. WHEN the Seed_Command completes successfully, THE Mockdb_Tool SHALL have inserted at least 1 row into each of the seven Mart_Relations.
2. THE Mockdb_Tool SHALL generate every value from Datafaker or from local deterministic logic, and SHALL NOT read, copy or derive values from real warehouse data or from any file under `.local/`.
3. THE Mockdb_Tool SHALL initialise its random generator from the Seed_Value, and SHALL use a fixed default Seed_Value when `--seed` is omitted.
4. WHEN the Seed_Command runs twice with the same Scale and Seed_Value on empty tables, THE Mockdb_Tool SHALL produce rows in the Local_Database whose column values are identical across both runs for every Mart_Relation (determinism property).
5. WHEN the Seed_Command runs with different Seed_Values and the same Scale, THE Mockdb_Tool SHALL produce the same row count in each Mart_Relation for every run.
6. WHEN `--scale` is omitted, THE Mockdb_Tool SHALL seed exactly 100 distinct library identifiers in `lims`.
7. WHEN `--scale N` is given with N an integer from 1 to 100000, THE Mockdb_Tool SHALL seed exactly N distinct library identifiers in `lims`.
8. IF `--scale` is given with a value that is not an integer from 1 to 100000, THEN THE Mockdb_Tool SHALL reject the command with an error indicating the permitted range, SHALL exit with a non-zero status, and SHALL insert no rows.
9. THE Mockdb_Tool SHALL write rows as jOOQ records using batch inserts.
10. FOR ALL generated values, THE Mockdb_Tool SHALL respect the column type and size, including NUMERIC(18,2) precision, BIGINT range, and date and timestamp types.
11. THE Mockdb_Tool SHALL generate library identifiers matching the pattern `L` followed by a two-digit year and a five-digit sequence number (for example `L2400001`).
12. THE Mockdb_Tool SHALL mark mock records so that a reader can recognise them as synthetic, using obviously fictional names and identifiers.
13. THE Mockdb_Tool SHALL NOT generate values that resemble real personal data of identifiable individuals.

### Requirement 7: Cross-table consistency

**User Story:** As a developer, I want joins across tables to return rows, so that I can test
features that combine resources.

#### Acceptance Criteria

1. WHEN the Mockdb_Tool generates a value for a subject, sample, library, sequencing run or portal run id column in any Mart_Relation, THE Mockdb_Tool SHALL select that value from the corresponding Key_Pool, so that 100% of generated values for those columns exist in their Key_Pool, and THE Mockdb_Tool SHALL NOT set any Key_Pool column to null.
2. THE Relationship_Spec SHALL state, for each Mart_Relation, every column that draws from a Key_Pool and the name of the Key_Pool it draws from.
3. FOR ALL seeded non-null `library_id` values in `fastq`, `bam`, `vcf` and `workflow`, THE Local_Database SHALL contain the same `library_id` in `lims` (referential consistency property).
4. FOR ALL seeded non-null `portal_run_id` values in `fastq`, `bam`, `vcf`, `workflow_output` and `workflow_cost_report`, THE Local_Database SHALL contain the same `portal_run_id` in `workflow` (referential consistency property).
5. WHEN a join between `lims` and each of `fastq`, `bam`, `vcf` and `workflow` on `library_id` is executed after seeding, THE Local_Database SHALL return at least one row for each of the four joins.
6. WHEN a join between `workflow` and each of `fastq`, `bam`, `vcf`, `workflow_output` and `workflow_cost_report` on `portal_run_id` is executed after seeding, THE Local_Database SHALL return at least one row for each of the five joins.
7. IF the Relationship_Spec names a table or column that does not exist in the Jooq_Model, THEN THE Mockdb_Tool SHALL fail before inserting any row, leave the Local_Database unchanged, print the table name and column name, and exit with a non-zero status.
8. IF a Mart_Relation has no entry in the Relationship_Spec, THEN THE Mockdb_Tool SHALL seed that table with type-valid values, print one warning naming the table, and exit with a zero status when no other error occurs.

### Requirement 8: Edge values for paging and sorting

**User Story:** As a developer, I want data with nulls and varied values, so that I exercise
paging, sorting and null handling.

#### Acceptance Criteria

1. WHEN the Mockdb_Tool generates rows for a table at any Scale, THE Mockdb_Tool SHALL set each nullable column that does not draw from a Key_Pool (per the Relationship_Spec) to null in at least 10% and at most 50% of that table's rows, except where the table has fewer than 10 rows, in which case at least 1 row SHALL have a null in that column.
2. THE Mockdb_Tool SHALL NOT set any Key_Pool column or any column declared non-nullable in the Jooq_Model to null in any generated row.
3. WHEN Scale is at least 11, THE Mockdb_Tool SHALL generate at least 11 rows in `lims`, so that the default page size of 10 yields at least 2 pages.
4. WHEN Scale is at least 5, THE Mockdb_Tool SHALL generate, in at least one sortable text column in `lims`, at least 2 distinct non-null values that each appear in 2 or more rows, so that sort tiebreaking is exercised. WHEN Scale is below 5, the null rule of criterion 1 SHALL take precedence over this tie rule.
5. WHEN Scale is at least 2, THE Mockdb_Tool SHALL generate `lims` data in which, for at least one sortable text column, the first row of an ascending sort and the first row of a descending sort have different values.
6. THE Mockdb_Tool SHALL include in each table at least one row holding the minimum boundary value and at least one row holding the maximum boundary value for each numeric and date column that the Jooq_Model declares, where the boundaries are zero (minimum) and the largest value the column's declared precision allows (maximum) for NUMERIC columns, and the earliest and latest seeded dates for date columns.
7. IF a column's declared type admits no valid boundary value for a table, or IF a table has too few rows to hold the null row required by criterion 1 and the two boundary rows required by criterion 6 for that column, THEN THE Mockdb_Tool SHALL skip the boundary row for that column and SHALL report the skipped table and column in its output without failing the run. For Scale below 3 this case can occur in `lims`.

### Requirement 9: Reset and idempotent bring-up

**User Story:** As a developer, I want to wipe and rebuild the mock data in one step, so that
I always return to a known state.

#### Acceptance Criteria

1. WHEN the Reset_Command runs against a Local_Database, THE Mockdb_Tool SHALL drop schema `mart` with cascade, SHALL recreate schema `mart` with all seven Mart_Relations empty so that every relation has 0 rows, and SHALL exit with status 0. The drop is permitted only after the localhost guard of criterion 6 has passed.
2. WHEN the Up_Command runs, THE Mockdb_Tool SHALL execute reset, then schema, then seed, in that order, and SHALL exit with status 0 only if all three steps succeed.
3. IF any step of the Up_Command fails, THEN THE Mockdb_Tool SHALL not execute the remaining steps, SHALL print a message naming the failed step, and SHALL exit with a non-zero status.
4. WHEN the Up_Command runs twice in succession with the same options, THE Local_Database SHALL contain identical row counts per relation in Mart_Relations and identical row content after each run (idempotence property).
5. IF the Seed_Command runs and any relation in Mart_Relations already contains at least 1 row, THEN THE Mockdb_Tool SHALL print a message that names the non-empty relation(s) and states the tables are not empty, SHALL insert 0 rows, and SHALL exit with a non-zero status.
6. IF the target host is not exactly `localhost`, `127.0.0.1` or `::1` when the Reset_Command, Schema_Command or Seed_Command runs, THEN THE Mockdb_Tool SHALL refuse to run the command, SHALL make 0 connections to the target host, SHALL leave the database unchanged, and SHALL exit with a non-zero status after printing a message naming the rejected host.
7. THE Mockdb_Tool SHALL NOT connect to the remote Aurora endpoint under any option.
8. THE Mockdb_Tool SHALL NOT read the `.env` file.

### Requirement 10: Dx helpers

**User Story:** As a developer, I want short shell helpers, so that I do not retype Compose
and Maven commands.

#### Acceptance Criteria

1. WHEN `nexus_docker_up` is invoked, THE Dx_Script SHALL start the Compose_Stack, wait until the healthcheck reports healthy for a maximum of 120 seconds, and then run the Up_Command once with the Mockdb_Tool's default scale and seed, taking no arguments.
2. IF the healthcheck does not report healthy within 120 seconds of `nexus_docker_up` starting the Compose_Stack, THEN THE Dx_Script SHALL print a message indicating the Local_Database did not become healthy, leave the Compose_Stack running, not run the Up_Command, and return a non-zero status.
3. WHEN `nexus_docker_down` is invoked, THE Dx_Script SHALL stop the Compose_Stack, keep the named volume with its data intact, and return the status of the stop command.
4. THE Dx_Script SHALL NOT parse or validate scale or seed, because the Mockdb_Tool does so (Requirement 4).
5. THE Dx_Script SHALL keep existing functions and their behaviour unchanged.
6. THE Dx_Script SHALL keep each new function a thin wrapper, consistent with the script's KISS rule.

### Requirement 11: Runtime verification of the Application

**User Story:** As a maintainer, I want proof that the Application works against the mock
data, so that the offline path is trustworthy.

#### Acceptance Criteria

1. WHEN the Application starts with the Mockdb_Profile on a free `SERVER_PORT` against a seeded Local_Database, THE Application SHALL report HTTP 200 on `/api/v1/system/info` within 60 seconds of process start.
2. WHEN `/api/v1/lims` is requested without paging parameters against a seeded Local_Database, THE Application SHALL return HTTP 200 with a `content` array containing at least 1 record and at most the default page size.
3. WHEN `/api/v1/lims?page=0&size=5` is requested against a Local_Database seeded with Scale 100, THE Application SHALL return exactly 5 records, `page.totalElements` equal to 100 and `page.totalPages` equal to 20.
4. WHEN `/api/v1/lims` is requested with a valid `sort` parameter in ascending order and then in descending order against a seeded Local_Database, THE Application SHALL return as the first record the record with the lowest sort-field value for ascending order and the highest sort-field value for descending order.
5. IF `/api/v1/lims` is requested with a `sort` field that is not a property of the LIMS resource, THEN THE Application SHALL respond with HTTP 400 with an error body identical in structure to the one returned against the remote database, and SHALL return no `content` records.
6. WHEN page 0 and page 1 are requested consecutively with the same `sort` parameter, using the library identifier as the sort field because its values are unique per record, and `size` of 5 against a Local_Database seeded with Scale 100, THE Application SHALL return 10 distinct records across the two pages, with no record appearing on both pages and no record skipped relative to the full sorted result (paging stability property).
7. IF the Local_Database is unreachable when the Application starts with the Mockdb_Profile, THEN THE Application SHALL fail startup with a non-zero exit status and an error indicating the database connection failure, using a startup check that does not require the schema name.
8. IF the Local_Database is reachable but the Mart_Relations do not exist, THEN THE Application SHALL respond to a request for `/api/v1/lims` with an error status and SHALL NOT return HTTP 200 with an empty `content` array. A seeded-but-empty Local_Database is not required to be distinguished from a seeded one.

### Requirement 12: Documentation and steering

**User Story:** As a maintainer, I want the docs to record the new path, so that the project
keeps one source of truth per fact.

#### Acceptance Criteria

1. THE Documentation SHALL include exactly one new ADR in `docs/adr/`, numbered as the next unused ADR number, that supersedes ADR 0005, states that the remote dev database reached through the tunnel remains available alongside the local mock database, records the local mock database as an offline path, and does not state which path is preferred, and briefly records the profile naming decision (`mockdb` and `tunnel`, retiring `local`).
2. THE ADR README index SHALL list the new ADR with its status, and SHALL show ADR 0005 with the status `Superseded by` followed by the new ADR number.
3. THE Documentation SHALL leave the body of ADR 0005 byte-for-byte unchanged except for its status line.
4. THE new ADR SHALL follow the template in `docs/adr/0000-template.md` and SHALL name at least one considered alternative (for example a static SQL dump, or Testcontainers) together with the reason it was not chosen.
5. THE Documentation SHALL include one recipe under `docs/dev/` with a separate section for each of: prerequisites, starting the Compose_Stack, running the Mockdb_Tool, starting the Application with the Mockdb_Profile, environment variables (the Mockdb_Env_Vars with their defaults, and the statement that the Tunnel_Env_Vars are ignored), a warning to update any `.env` or IDE run configuration that still selects the profile `local`, and troubleshooting.
6. THE recipe troubleshooting section SHALL list at least three failure symptoms, each with its cause and its resolution.
7. THE `docs/README.md` index SHALL link to the new recipe, and the link SHALL resolve to an existing file.
8. THE `docs/architecture/overview.md` SHALL list `orcanexus-mockdb` in the module table and in the dependency diagram, and SHALL state that no runtime module depends on it.
9. THE steering files `structure.md`, `tech.md` and `jooq.md` SHALL describe the new module, the new tooling and the mockdb verification notes respectively, each addition SHALL be at most 10 lines, and each SHALL link to `docs/` instead of duplicating prose.
10. THE `README.md` SHALL remain at most 40 lines and SHALL add at most one link, which points to the new recipe.
11. THE Documentation SHALL NOT contain endpoint paths, request parameters or response schemas copied from OpenAPI.
12. THE Documentation, Java code and Java comments SHALL NOT contain concrete database schema names, except as configuration values already established by `jooq.md`.
13. WHEN the documentation change is complete, every relative link added or modified by this requirement SHALL resolve to an existing file or heading.

### Requirement 13: Quality gates and regression safety

**User Story:** As a maintainer, I want the existing build to stay green, so that the feature
adds no hidden cost.

#### Acceptance Criteria

1. WHEN the Quality_Gates (tests, Spotless, Checkstyle and SpotBugs) run via `mvn verify` after the change, THE Build SHALL complete with exit code 0 and zero Quality_Gates violations.
2. WHEN `mvn verify` runs on a machine where Docker is not installed and no Local_Database is reachable, THE Build SHALL complete with exit code 0.
3. WHERE a test needs a database, THE test SHALL be reported as skipped, or SHALL run against an isolated in-process or test-scoped database, so that `mvn verify` completes with exit code 0 without Docker and without a Local_Database.
4. WHEN `mvn verify` runs without a database, THE Mockdb_Tool unit tests SHALL cover argument parsing for each supported argument, including at least one valid input, one missing required argument, and one invalid value, and each case SHALL pass.
5. WHEN the Mockdb_Tool generates values twice with the same seed and the same inputs, THE unit tests SHALL assert that both runs produce identical values, and WHEN the seed differs, THE unit tests SHALL assert that at least one generated value differs. These tests SHALL run without a database.
6. WHEN `mvn verify` runs, THE Mockdb_Tool unit test SHALL assert that every table and column named in each Relationship_Spec exists in the Jooq_Model, and IF any named table or column is missing, THEN THE test SHALL fail with a message identifying the missing table and column.
7. THE change SHALL NOT delete, disable, skip or weaken any test that existed before the change, so that the count of pre-existing tests executed and passing after the change is greater than or equal to the count before the change.
8. THE change SHALL NOT modify any file under the generated jOOQ package, and Java sources changed by the feature SHALL be formatted only by `mvn spotless:apply`, so that `mvn spotless:check` reports zero violations.

### Requirement 14: Profile rename: local to tunnel

**User Story:** As a developer, I want each Spring profile named after its data source mechanism, so that `tunnel` and `mockdb` are unambiguous and `local` no longer suggests the wrong database.

#### Acceptance Criteria

1. THE Application SHALL provide the Tunnel_Profile settings in the file `application-tunnel.yaml`, renamed from `application-local.yaml` with git rename detection preserving the file history, and the file SHALL activate on the profile name `tunnel`.
2. THE Tunnel_Profile SHALL keep every setting of the former `application-local.yaml` with an unchanged key and value, except the profile name and the value of `orcanexus.system.environment`, which SHALL be `tunnel` instead of `local`, so that the driver class, URL format, IAM authentication method, schema mappings and `devtools` settings are identical to those of the former `local` profile.
3. WHEN no Spring profile is selected, THE Application SHALL activate the Tunnel_Profile, and the datasource behaviour SHALL be identical to the behaviour of the former default `local` profile.
4. THE Application SHALL define no profile named `local`, and THE Application SHALL provide no alias or fallback from `local` to any other profile.
5. IF the profile `local` is selected, THEN THE Application SHALL treat it as an unknown profile with no profile-specific settings and apply only the base configuration, so that neither the Tunnel_Profile settings nor the Mockdb_Profile settings take effect.
6. THE Repository SHALL contain zero matches for the Spring profile name `local` and for the string `application-local` in the Searched_Locations, where the Searched_Locations are the tracked files in: `.kiro/steering/`, `docs/`, `README.md`, `README_DEV.md`, `dx.sh`, `.github/`, `orcanexus-application/src/`, `.env.example`, `.kiro/specs/` and the tests, and where the Exclusions are exactly: (a) the `.local/` directory, (b) the bodies of accepted ADRs, (c) the new ADR and the new recipe under `docs/dev/`, only in the passages that describe the rename, (d) the spec `.kiro/specs/offline-mockdb/`, and (e) the delivered specs under `.kiro/specs/` other than `offline-mockdb`, including `.kiro/specs/spring-pagination/`.
7. WHERE an IDE run configuration is tracked in git, THE Repository SHALL contain no reference to the Spring profile name `local` in that run configuration.
8. THE Documentation SHALL leave the body of every accepted ADR unchanged, including ADR 0005, except for the status line permitted by Requirement 12.
9. THE Documentation SHALL record the rename and its reason (the name `local` is ambiguous between the remote dev database reached through a local tunnel and a local database) in the new ADR and in the recipe under `docs/dev/`, and THE recipe SHALL warn developers to update any `.env` or IDE run configuration that still selects `local`.
10. WHEN a case-sensitive search is run over the Searched_Locations from criterion 6, with the Exclusions from criterion 6 applied, for a profile selection of `local` (such as `spring.profiles.active=local`, `-Dspring.profiles.active=local`, `SPRING_PROFILES_ACTIVE=local`, `--spring.profiles.active=local`, or `local` in a profile list) and for the string `application-local`, THE search SHALL return zero matches. The English word "local" in other phrases (such as Local_Database, "local tunnel", or the `.local/` directory) SHALL NOT count as a match.
11. WHEN `mvn -DskipTests compile` and `mvn verify` run after the rename, THE Build SHALL complete with exit code 0.
12. THE rename SHALL NOT require reading the `.env` file, and IF a reference in `.env` needs checking, THEN the developer SHALL be asked for permission before the file is read.
13. THE file `.env.example` SHALL set `SPRING_PROFILES_ACTIVE` to `tunnel`.

## Resolved Decisions and Verified Facts

- **DDL generation.** `DSLContext.ddl(Mart.MART)` on the committed model (jOOQ 3.21.7) emits plain PostgreSQL: `create schema "mart"` and `create table "mart"."<name>"` for seven tables. Column types are text, date, timestamp(6), bigint, numeric(18,2) in `workflow_cost_report`, and numeric(12,4) for `workflow.workflow_duration`. The model declares no keys and every column is nullable. The output has no IF NOT EXISTS by default, so idempotent creation uses the jOOQ DDL export flags (createSchemaIfNotExists, createTableIfNotExists) or an equivalent; this is a design detail.
- **PostgreSQL image.** The user chose version 18.6. This is the user's decision and is not derived from the Aurora version. From PostgreSQL 18 the official image uses a versioned data directory, so the named volume is mounted at `/var/lib/postgresql`; mounting `/var/lib/postgresql/data` silently creates an anonymous volume and loses data on recreate.
- **Datafaker.** Datafaker 2.x requires Java 17 or later. The latest reported version is 2.7.0 (from secondary sources); confirm it on Maven Central when pinning, since Spring Boot does not manage it.
- **PostgreSQL driver.** Spring Boot manages the `org.postgresql` driver version; `orcanexus-mockdb` reuses it.
- **Profile layout.** `application-dev.yaml` and `application-prod.yaml` do not override the datasource and use the base IAM wrapper configuration with `DB_HOST` and related variables. `application-mockdb.yaml` fully overrides `driver-class-name`, `url`, `username` and `password` of the base datasource block; Hikari settings may be inherited. The base `orcanexus.datasource.schema-mappings` default (`mart` to `mart`) is an identity mapping and applies no effective rewrite.
- **Current configuration.** `application.yaml` sets `spring.profiles.default: local` and `orcanexus.system.environment: ${SPRING_PROFILES_ACTIVE:unknown}`. `application-local.yaml` sets `orcanexus.system.environment: local`. `.env.example` sets `SPRING_PROFILES_ACTIVE=local`. Requirement 14 replaces these values with `tunnel`.
- **Environment variables.** The mockdb path uses the separate Mockdb_Env_Vars and ignores the Tunnel_Env_Vars (Requirements 2 and 4).
- **Reset strategy.** Drop schema `mart` with cascade and recreate it, guarded by the localhost-only check (Requirement 9).
- **Default host port.** 5433 (Requirement 1).
- **Environment label.** The Tunnel_Profile sets `orcanexus.system.environment` to `tunnel`; the Mockdb_Profile sets `mockdb`.
- **Column sets.** Key-like columns are `library_id` (lims, fastq, bam, vcf, workflow) and `portal_run_id` (bam, fastq, vcf, workflow, workflow_output, workflow_cost_report), plus `cohort_id`, `bucket` and `key` in the artifact tables. The full column list is derived from the Jooq_Model at design time.
- **Tooling on the dev machine.** Docker 29 with Compose v5 and Java 21 are installed.

## Remaining Open Points

- **PostgreSQL image tag.** That `postgres:18.6` exists on Docker Hub is confirmed in task 1.1.
- **Paging tiebreaker on real data.** The existing `LimsRepository` appends the first column (`sequencing_run_id`, non-unique) as the sort tiebreaker, so paging with a non-unique sort key may be unstable on real data too. This is outside this spec and should be raised separately. Requirement 11 criterion 6 therefore sorts by the unique library identifier.
- **DDL execution: verified in task 1.** PostgreSQL 18.6 accepted the jOOQ-generated DDL twice in one transaction; no fallback was needed. Every relation is marked `TableOptions.table()` although the real warehouse objects are views.
- **Reference list for the rename.** A search during this requirements pass found references to the old name `local`, outside `.local/` and accepted ADR bodies, in `application.yaml`, `application-local.yaml`, `.env.example`, `.kiro/steering/tech.md`, and the delivered spec `.kiro/specs/spring-pagination/` (design.md and tasks.md, excluded from the zero-match search). Design must repeat the search, including tests, CI workflows and tracked IDE configurations. The `.env` file must not be read without the developer's permission; a developer whose `.env` sets `SPRING_PROFILES_ACTIVE=local` must update it by hand.
- **Files to re-read before editing.** `application-tunnel.yaml` (formerly `application-local.yaml`), `dx.sh`, `application.yaml`, `application-dev.yaml`, `application-prod.yaml` and the module POMs.
- **Out of scope.** Any change to the existing `LimsRepository` (including its sort tiebreaker), real warehouse data, Testcontainers-based tests, CI use of the Compose_Stack, write endpoints, and any change to the remote development path other than the profile rename in Requirement 14.
